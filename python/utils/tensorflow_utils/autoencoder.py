import tensorflow as tf
from sklearn.model_selection import KFold
from tensorflow import keras
import numpy as np
import pandas as pd
import itertools
import tqdm
from tqdm.contrib.itertools import product


from utils.paralellization_util import process_jobs_joblib

DEFAULT_METRICS = [
    tf.keras.metrics.RootMeanSquaredError(),
    tf.keras.metrics.MeanSquaredError(),
]


class ExecutorRun:
    def __init__(
        self,
        hidden_layers,
        hidden_layer_neurons,
        epoch,
        batch_size,
        learning_rate,
        momentum,
        activation_function,
        kernel_initializer,
        loss_function,
        reduction_output_neurons,
        X_transformed,
        n_folds,
        shuffle,
        metrics,
    ):
        self.hidden_layers = hidden_layers
        self.hidden_layer_neurons = hidden_layer_neurons
        self.epoch = epoch
        self.batch_size = batch_size
        self.learning_rate = learning_rate
        self.momentum = momentum
        self.activation_function = activation_function
        self.kernel_initializer = kernel_initializer
        self.loss_function = loss_function
        self.reduction_output_neurons = reduction_output_neurons

        self.X_transformed = X_transformed
        self.n_folds = n_folds
        self.shuffle = shuffle
        self.metrics = metrics

        self.mse = None
        self.rmse = None
        self.finished = False

    def run(self):
        self.finished = False
        try:
            self.mse, self.rmse = get_out_of_sample_metrics(
                X_transformed=self.X_transformed,
                reduction_output_neurons=self.reduction_output_neurons,
                hidden_layer_neurons=self.hidden_layer_neurons,
                hidden_layers=self.hidden_layers,
                learning_rate=self.learning_rate,
                momentum=self.momentum,
                epoch=self.epoch,
                batch_size=self.batch_size,
                n_splits=self.n_folds,
                shuffle=self.shuffle,
                loss_function=self.loss_function,
                kernel_initializer=self.kernel_initializer,
                activation_function=self.activation_function,
                metrics=self.metrics,
            )
        except Exception as e:
            print(rf"error running ExecutorRun {e}")
        finally:
            self.finished = True

    def get_df(self) -> pd.DataFrame:
        rows = []
        rows.append(
            [
                self.hidden_layers,
                self.hidden_layer_neurons,
                self.learning_rate,
                self.momentum,
                self.epoch,
                self.batch_size,
                self.n_folds,
                self.loss_function,
                self.reduction_output_neurons,
                self.kernel_initializer,
                self.activation_function,
                self.mse,
                self.rmse,
            ]
        )
        df = pd.DataFrame(
            rows,
            columns=[
                'hidden_layers',
                'hidden_layer_neurons',
                'learning_rate',
                'momentum',
                'epoch',
                'batch_size',
                'n_folds',
                'loss_function',
                'reduction_output_neurons',
                'kernel_initializer',
                'activation_function',
                self.metrics[0].name,
                self.metrics[1].name,
            ],
        )
        return df


def scale_input_data(X_input: pd.DataFrame) -> pd.DataFrame:
    from sklearn.preprocessing import StandardScaler

    scaler = StandardScaler()
    return scaler.fit_transform(X_input)


def get_out_of_sample_metrics(
    X_transformed: pd.DataFrame,
    reduction_output_neurons: int,
    hidden_layer_neurons: int,
    hidden_layers: int,
    learning_rate: float,
    momentum: float,
    epoch: int,
    batch_size: int,
    n_splits: int,
    shuffle=True,
    loss_function: str = 'mse',
    kernel_initializer: str = 'GlorotNormal',
    activation_function: str = 'sigmoid',
    metrics: list = None,
    output_activation_function: str = None,
) -> (float, float):
    if metrics is None:
        metrics = DEFAULT_METRICS
    model = create_model_complete(
        X_transformed=X_transformed,
        reduction_output_neurons=reduction_output_neurons,
        hidden_layers=hidden_layers,
        hidden_layers_neurons=hidden_layer_neurons,
        activation_hidden_function=activation_function,
        output_activation_function=output_activation_function,
        learning_rate=learning_rate,
        momentum=momentum,
        loss=loss_function,
        metrics=metrics,
        kernel_initializer=kernel_initializer,
    )

    kf = KFold(n_splits=n_splits, shuffle=shuffle)

    evaluation_1 = []
    evaluation_2 = []
    for train_index, test_index in kf.split(X_transformed):
        X_train_fold = X_transformed.iloc[train_index]
        X_test_fold = X_transformed.iloc[test_index]

        y_train_fold = X_transformed.iloc[train_index]
        y_test_fold = X_transformed.iloc[test_index]
        model.fit(
            X_train_fold.values,
            y_train_fold.fillna(0).values,
            epochs=epoch,
            verbose=0,
            batch_size=batch_size,
        )  # verbose 0 to zero logs each epoch
        model_test_loss, model_test_rmse, model_test_mse = model.evaluate(
            X_test_fold.values, y_test_fold.fillna(0).values, verbose=2
        )
        evaluation_1.append(model_test_mse)
        evaluation_2.append(model_test_rmse)

    return np.mean(evaluation_1), np.mean(evaluation_2)


def parameter_tuning(
    X_transformed: pd.DataFrame,
    hidden_layers_list,
    hidden_layer_neurons_list,
    epoch_list,
    batch_size_list,
    learning_rate_list,
    momentum_list,
    activation_function_list,
    kernel_initializer_list,
    loss_function_list,
    reduction_output_neurons_list,
    n_folds,
    shuffle=True,
    metrics: list = None,
    X_transformed_test: pd.DataFrame = None,
    take_best: int = 5,
    n_jobs: int = -5,
):
    import os

    if n_jobs < 0:
        cpus = os.cpu_count()
        n_jobs = cpus + n_jobs

    iterative = itertools.product(
        hidden_layers_list,
        hidden_layer_neurons_list,
        epoch_list,
        batch_size_list,
        learning_rate_list,
        momentum_list,
        activation_function_list,
        kernel_initializer_list,
        loss_function_list,
        reduction_output_neurons_list,
    )
    len_iterations = len(list(iterative))

    print(rf"{len_iterations} iterations running in parallel on {n_jobs} n_jobs ")
    executors = []
    running_jobs = 0
    jobs = []
    for (
        hidden_layers,
        hidden_layer_neurons,
        epoch,
        batch_size,
        learning_rate,
        momentum,
        activation_function,
        kernel_initializer,
        loss_function,
        reduction_output_neurons,
    ) in product(
        hidden_layers_list,
        hidden_layer_neurons_list,
        epoch_list,
        batch_size_list,
        learning_rate_list,
        momentum_list,
        activation_function_list,
        kernel_initializer_list,
        loss_function_list,
        reduction_output_neurons_list,
    ):
        executor_job = ExecutorRun(
            hidden_layers=hidden_layers,
            hidden_layer_neurons=hidden_layer_neurons,
            epoch=epoch,
            batch_size=batch_size,
            learning_rate=learning_rate,
            momentum=momentum,
            activation_function=activation_function,
            kernel_initializer=kernel_initializer,
            loss_function=loss_function,
            reduction_output_neurons=reduction_output_neurons,
            n_folds=n_folds,
            shuffle=shuffle,
            X_transformed=X_transformed,
            metrics=metrics,
        )
        job = {"func": executor_job.run}
        jobs.append(job)
    process_jobs_joblib(jobs=jobs, num_threads=n_jobs)
    # #waiting finished
    # all_executors_are_finished=False
    # while not all_executors_are_finished:
    #
    #     all_executors_are_finished=True
    #     for executor in executors:
    #         if(not executor.finished):
    #             all_executors_are_finished=False

    # finished all
    print(rf"finished all parameter tuning ")
    output_df = None
    import utils.pandas_utils.dataframe_utils

    for executor in executors:
        if output_df is None:
            output_df = executor.get_df()
        else:
            output_df = utils.pandas_utils.dataframe_utils.join_by_row(
                output_df, executor.get_df()
            )
    # sort it
    OOS_kfold_results = output_df.sort_values(
        metrics[0].name, ascending=True
    ).reset_index(
        drop=True
    )  # lower mse is better in TF

    ## test
    if X_transformed_test is not None:
        print(rf'validating best results on  {take_best} test')
        print(OOS_kfold_results.head())

        inputs_validating = []
        evaluation_1 = []
        evaluation_2 = []
        for row in tqdm.tqdm(range(take_best)):
            hidden_layers = int(OOS_kfold_results.iloc[row]['hidden_layers'])
            hidden_layer_neurons = int(
                OOS_kfold_results.iloc[row]['hidden_layer_neurons']
            )
            learning_rate = float(OOS_kfold_results.iloc[row]['learning_rate'])
            momentum = float(OOS_kfold_results.iloc[row]['momentum'])
            epoch = int(OOS_kfold_results.iloc[row]['epoch'])
            batch_size = int(OOS_kfold_results.iloc[row]['batch_size'])
            loss_function = str(OOS_kfold_results.iloc[row]['loss_function'])
            reduction_output_neurons = int(
                OOS_kfold_results.iloc[row]['reduction_output_neurons']
            )
            kernel_initializer = str(OOS_kfold_results.iloc[row]['kernel_initializer'])
            activation_function = str(
                OOS_kfold_results.iloc[row]['activation_function']
            )
            training_score_1 = str(OOS_kfold_results.iloc[row][metrics[0].name])
            training_score_2 = str(OOS_kfold_results.iloc[row][metrics[1].name])

            X_train_fold = X_transformed
            X_test_fold = X_transformed_test

            y_train_fold = X_transformed
            y_test_fold = X_transformed_test
            model = create_model_complete(
                X_transformed=X_transformed,
                reduction_output_neurons=reduction_output_neurons,
                hidden_layers=hidden_layers,
                hidden_layers_neurons=hidden_layer_neurons,
                activation_hidden_function=activation_function,
                learning_rate=learning_rate,
                momentum=momentum,
                loss=loss_function,
                metrics=metrics,
                kernel_initializer=kernel_initializer,
            )

            model.fit(
                X_train_fold.values,
                y_train_fold.fillna(0).values,
                epochs=epoch,
                verbose=0,
                batch_size=batch_size,
            )  # verbose 0 to zero logs each epoch
            model_test_loss, model_test_rmse, model_test_mse = model.evaluate(
                X_test_fold.values, y_test_fold.fillna(0).values, verbose=2
            )
            evaluation_1.append(model_test_mse)
            evaluation_2.append(model_test_rmse)
            inputs_validating.append(
                [
                    hidden_layers,
                    hidden_layer_neurons,
                    learning_rate,
                    momentum,
                    epoch,
                    batch_size,
                    loss_function,
                    reduction_output_neurons,
                    kernel_initializer,
                    activation_function,
                    training_score_1,
                    training_score_2,
                    model,
                ]
            )
        OOS_kfold_results_validation = pd.DataFrame(
            inputs_validating,
            columns=[
                'hidden_layers',
                'hidden_layer_neurons',
                'learning_rate',
                'momentum',
                'epoch',
                'batch_size',
                'loss_function',
                'reduction_output_neurons',
                'kernel_initializer',
                'activation_function',
                rf'training_{metrics[0].name}',
                rf'training_{metrics[1].name}',
                'model',
            ],
        )
        OOS_kfold_results_validation[metrics[0].name] = evaluation_1
        OOS_kfold_results_validation[metrics[1].name] = evaluation_2
        OOS_kfold_results_validation = OOS_kfold_results_validation.sort_values(
            metrics[0].name, ascending=True
        ).reset_index(
            drop=True
        )  # mse is better the lower
        return OOS_kfold_results_validation

    return OOS_kfold_results


def old_parameter_tuning(
    X_transformed: pd.DataFrame,
    hidden_layers_list,
    hidden_layer_neurons_list,
    epoch_list,
    batch_size_list,
    learning_rate_list,
    momentum_list,
    activation_function_list,
    kernel_initializer_list,
    loss_function_list,
    reduction_output_neurons_list,
    n_folds,
    shuffle=True,
    metrics: list = None,
    X_transformed_test: pd.DataFrame = None,
    take_best: int = 5,
):
    if metrics is None:
        metrics = DEFAULT_METRICS

    iterative = itertools.product(
        hidden_layers_list,
        hidden_layer_neurons_list,
        epoch_list,
        batch_size_list,
        learning_rate_list,
        momentum_list,
        activation_function_list,
        kernel_initializer_list,
        loss_function_list,
        reduction_output_neurons_list,
    )
    len_iterations = len(list(iterative))
    print(
        rf"we are going to train the autoencoder nn  {len_iterations} times on {n_folds} n_folds "
    )
    inputs = []
    evaluation_1 = []
    evaluation_2 = []

    for (
        hidden_layers,
        hidden_layer_neurons,
        epoch,
        batch_size,
        learning_rate,
        momentum,
        activation_function,
        kernel_initializer,
        loss_function,
        reduction_output_neurons,
    ) in product(
        hidden_layers_list,
        hidden_layer_neurons_list,
        epoch_list,
        batch_size_list,
        learning_rate_list,
        momentum_list,
        activation_function_list,
        kernel_initializer_list,
        loss_function_list,
        reduction_output_neurons_list,
    ):
        mse_iter, rmse_iter = get_out_of_sample_metrics(
            X_transformed=X_transformed,
            reduction_output_neurons=reduction_output_neurons,
            hidden_layer_neurons=hidden_layer_neurons,
            hidden_layers=hidden_layers,
            learning_rate=learning_rate,
            momentum=momentum,
            epoch=epoch,
            batch_size=batch_size,
            n_splits=n_folds,
            shuffle=shuffle,
            loss_function=loss_function,
            kernel_initializer=kernel_initializer,
            activation_function=activation_function,
            metrics=metrics,
        )

        inputs.append(
            [
                hidden_layers,
                hidden_layer_neurons,
                learning_rate,
                momentum,
                epoch,
                batch_size,
                n_folds,
                loss_function,
                reduction_output_neurons,
                kernel_initializer,
                activation_function,
            ]
        )
        evaluation_1.append(mse_iter)
        evaluation_2.append(rmse_iter)

    # kfold out of sample analyisis -> training set
    OOS_kfold_results = pd.DataFrame(
        inputs,
        columns=[
            'hidden_layers',
            'hidden_layer_neurons',
            'learning_rate',
            'momentum',
            'epoch',
            'batch_size',
            'n_folds',
            'loss_function',
            'reduction_output_neurons',
            'kernel_initializer',
            'activation_function',
        ],
    )
    OOS_kfold_results[metrics[0].name] = evaluation_1
    OOS_kfold_results[metrics[1].name] = evaluation_2
    OOS_kfold_results = OOS_kfold_results.sort_values(
        metrics[0].name, ascending=True
    ).reset_index(
        drop=True
    )  # lower mse is better in TF

    ## test
    if X_transformed_test is not None:
        print('validating best results on test')
        print(OOS_kfold_results.head())

        inputs_validating = []
        evaluation_1 = []
        evaluation_2 = []
        for row in tqdm.tqdm(range(take_best)):
            hidden_layers = int(OOS_kfold_results.iloc[row]['hidden_layers'])
            hidden_layer_neurons = int(
                OOS_kfold_results.iloc[row]['hidden_layer_neurons']
            )
            learning_rate = float(OOS_kfold_results.iloc[row]['learning_rate'])
            momentum = float(OOS_kfold_results.iloc[row]['momentum'])
            epoch = int(OOS_kfold_results.iloc[row]['epoch'])
            batch_size = int(OOS_kfold_results.iloc[row]['batch_size'])
            loss_function = str(OOS_kfold_results.iloc[row]['loss_function'])
            reduction_output_neurons = int(
                OOS_kfold_results.iloc[row]['reduction_output_neurons']
            )
            kernel_initializer = str(OOS_kfold_results.iloc[row]['kernel_initializer'])
            activation_function = str(
                OOS_kfold_results.iloc[row]['activation_function']
            )
            training_score_1 = str(OOS_kfold_results.iloc[row][metrics[0].name])
            training_score_2 = str(OOS_kfold_results.iloc[row][metrics[1].name])

            X_train_fold = X_transformed
            X_test_fold = X_transformed_test

            y_train_fold = X_transformed
            y_test_fold = X_transformed_test
            model = create_model_complete(
                X_transformed=X_transformed,
                reduction_output_neurons=reduction_output_neurons,
                hidden_layers=hidden_layers,
                hidden_layers_neurons=hidden_layer_neurons,
                activation_hidden_function=activation_function,
                learning_rate=learning_rate,
                momentum=momentum,
                loss=loss_function,
                metrics=metrics,
                kernel_initializer=kernel_initializer,
            )

            model.fit(
                X_train_fold.values,
                y_train_fold.fillna(0).values,
                epochs=epoch,
                verbose=0,
                batch_size=batch_size,
            )  # verbose 0 to zero logs each epoch
            model_test_loss, model_test_rmse, model_test_mse = model.evaluate(
                X_test_fold.values, y_test_fold.fillna(0).values, verbose=2
            )
            evaluation_1.append(model_test_mse)
            evaluation_2.append(model_test_rmse)
            inputs_validating.append(
                [
                    hidden_layers,
                    hidden_layer_neurons,
                    learning_rate,
                    momentum,
                    epoch,
                    batch_size,
                    loss_function,
                    reduction_output_neurons,
                    kernel_initializer,
                    activation_function,
                    training_score_1,
                    training_score_2,
                    model,
                ]
            )
        OOS_kfold_results_validation = pd.DataFrame(
            inputs_validating,
            columns=[
                'hidden_layers',
                'hidden_layer_neurons',
                'learning_rate',
                'momentum',
                'epoch',
                'batch_size',
                'loss_function',
                'reduction_output_neurons',
                'kernel_initializer',
                'activation_function',
                rf'training_{metrics[0].name}',
                rf'training_{metrics[1].name}',
                'model',
            ],
        )
        OOS_kfold_results_validation[metrics[0].name] = evaluation_1
        OOS_kfold_results_validation[metrics[1].name] = evaluation_2
        OOS_kfold_results_validation = OOS_kfold_results_validation.sort_values(
            metrics[0].name, ascending=True
        ).reset_index(
            drop=True
        )  # mse is better the lower
        return OOS_kfold_results_validation
    return OOS_kfold_results


def create_model_complete(
    X_transformed: pd.DataFrame,
    reduction_output_neurons: int,
    hidden_layers: int = 1,
    hidden_layers_neurons: int = -1,
    activation_hidden_function='sigmoid',
    output_activation_function: str = 'sigmoid',
    learning_rate: float = 0.01,
    momentum: float = 0.0,
    kernel_initializer: str = 'GlorotNormal',
    loss: str = 'mse',
    metrics=None,
) -> keras.models.Model:
    if metrics is None:
        metrics = DEFAULT_METRICS

    if hidden_layers_neurons < 0:
        hidden_layers_neurons = int(
            (X_transformed.shape[1] + reduction_output_neurons) * 2
        )

    print(
        rf"create autoencoder from {X_transformed.shape[1]} to {reduction_output_neurons} with {hidden_layers} hidden_layers of {hidden_layers_neurons} neurons"
    )

    model = keras.Sequential()
    input_layer = keras.layers.Flatten(input_shape=(X_transformed.shape[1],))
    model.add(input_layer)

    for layer in range(hidden_layers):
        hidden_middle_layer = keras.layers.Dense(
            hidden_layers_neurons,
            activation=activation_hidden_function,
            kernel_initializer=kernel_initializer,
        )
        model.add(hidden_middle_layer)

    hidden_output_layer = keras.layers.Dense(
        reduction_output_neurons, activation=output_activation_function
    )
    model.add(hidden_output_layer)

    # hidden layers before output_layer
    for layer in range(hidden_layers):
        hidden_middle_layer = keras.layers.Dense(
            hidden_layers_neurons,
            activation=activation_hidden_function,
            kernel_initializer=kernel_initializer,
        )
        model.add(hidden_middle_layer)

    # output layer same as input
    output_layer = keras.layers.Dense(X_transformed.shape[1])
    model.add(output_layer)

    # compile model
    optimizer = keras.optimizers.SGD(learning_rate=learning_rate)
    if momentum > 0:
        optimizer = keras.optimizers.SGD(
            learning_rate=learning_rate, momentum=momentum, nesterov=True
        )
    model.compile(
        optimizer=optimizer,
        loss=loss,
        metrics=metrics,
    )
    return model


def get_autoencoder_model(
    complete_model: keras.models.Model,
    hidden_layers: int,
    activation_hidden_function: str,
    output_activation_function: str,
) -> keras.models.Model:
    model2 = keras.Sequential()

    # INPUT
    layer_counter = 0
    neurons = complete_model.layers[layer_counter].input_shape[1]
    weights = complete_model.layers[layer_counter].get_weights()
    input_layer = keras.layers.Flatten(input_shape=(neurons,), weights=weights)
    model2.add(input_layer)
    layer_counter += 1

    # HIDDEN
    for layer in range(hidden_layers):
        hidden_neurons = complete_model.layers[layer_counter].output_shape[1]
        weights = complete_model.layers[layer_counter].get_weights()
        hidden_middle_layer = keras.layers.Dense(
            hidden_neurons, activation=activation_hidden_function, weights=weights
        )
        model2.add(hidden_middle_layer)
        layer_counter += 1

    # OUTPUT
    output_neurons = complete_model.layers[layer_counter].output_shape[1]
    output_weights = complete_model.layers[layer_counter].get_weights()

    output_layer = keras.layers.Dense(
        output_neurons, activation=output_activation_function, weights=output_weights
    )
    model2.add(output_layer)

    return model2


def create_fit_model(
    X_transformed: pd.DataFrame,
    reduction_output_neurons: int,
    hidden_layers: int = 1,
    hidden_layers_neurons: int = -1,
    activation_hidden_function='sigmoid',
    output_activation_function: str = 'sigmoid',
    learning_rate: float = 0.01,
    momentum: float = 0.0,
    epochs=10,
    loss: str = 'mse',
    kernel_initializer: str = 'GlorotNormal',
    metrics: list = None,
) -> keras.models.Model:
    if metrics is None:
        metrics = DEFAULT_METRICS

    print(
        rf"create autoencoder from {X_transformed.shape[1]} to {reduction_output_neurons} with {hidden_layers} hidden_layers of {hidden_layers_neurons} neurons"
    )
    model = create_model_complete(
        X_transformed=X_transformed,
        reduction_output_neurons=reduction_output_neurons,
        hidden_layers=hidden_layers,
        hidden_layers_neurons=hidden_layers_neurons,
        activation_hidden_function=activation_hidden_function,
        output_activation_function=output_activation_function,
        learning_rate=learning_rate,
        momentum=momentum,
        kernel_initializer=kernel_initializer,
        loss=loss,
        metrics=metrics,
    )

    # train the model complete
    try:
        from tensorflow_core.python.keras.callbacks import EarlyStopping
    except:
        from tensorflow.python.keras.callbacks import EarlyStopping

    treshold_index = int(len(X_transformed) * 0.75)
    validation_data = X_transformed.iloc[treshold_index:, :]
    train_data = X_transformed.iloc[:treshold_index, :]
    print(
        f'training on {len(train_data)} rows and validating on {len(validation_data)} rows'
    )
    es = EarlyStopping(
        monitor='val_loss', min_delta=0.0001, mode='auto', verbose=1, patience=3
    )
    model.fit(
        np.asarray(train_data),
        np.asarray(train_data),
        validation_data=(np.asarray(validation_data), np.asarray(validation_data)),
        epochs=epochs,
        verbose=2,
        callbacks=[es],
    )

    # split the model , take the first part
    model2 = get_autoencoder_model(
        complete_model=model,
        hidden_layers=hidden_layers,
        activation_hidden_function=activation_hidden_function,
        output_activation_function=output_activation_function,
    )

    optimizer = keras.optimizers.SGD(learning_rate=learning_rate)
    if momentum > 0:
        optimizer = keras.optimizers.SGD(
            learning_rate=learning_rate, momentum=momentum, nesterov=True
        )
    model2.compile(
        optimizer=optimizer,
        loss=loss,
        metrics=metrics,
    )

    return model2


def save_model(file, model):
    import utils.tensorflow_utils.utils

    utils.tensorflow_utils.utils.save_model(file, model=model)


def load_model(file):
    import utils.tensorflow_utils.utils

    return utils.tensorflow_utils.utils.load_model(file)


def get_features(
    model: keras.models.Model,
    input_df: pd.DataFrame,
    prefix_column: str = 'autoencoder',
) -> (pd.DataFrame):
    import copy

    output_arr = model.predict(input_df.values)
    new_features = [rf'{prefix_column}_{i}' for i in range(output_arr.shape[1])]
    output_df = pd.DataFrame(output_arr, columns=new_features, index=input_df.index)
    return output_df


if __name__ == '__main__':
    (x_train, y_train), (x_test, y_test) = keras.datasets.mnist.load_data()
    x_train_df = pd.DataFrame(x_train[:, :, 1])  # 60000x28
    x_test_df = pd.DataFrame(x_test[:, :, 1])  # 10000x28

    # OOS_kfold_results = parameter_tuning(X_transformed=x_train_df, hidden_layers_list=[2],
    #                                      hidden_layer_neurons_list=[10], epoch_list=[4, 5],
    #                                      batch_size_list=[32],
    #                                      learning_rate_list=[0.01], momentum_list=[0.0],
    #                                      activation_function_list=['sigmoid', 'relu'],
    #                                      kernel_initializer_list=['GlorotNormal'],
    #                                      loss_function_list=[tf.keras.losses.MeanSquaredError()],
    #                                      reduction_output_neurons_list=[3, 6], n_folds=3
    #                                      )
    # best_activation = OOS_kfold_results.iloc[0]['activation_function']
    # best_reduction_neurons = OOS_kfold_results.iloc[0]['reduction_output_neurons']
    # best_epoch = OOS_kfold_results.iloc[0]['epoch']
    results_df = parallel_parameter_tuning(
        X_transformed=x_train_df,
        hidden_layers_list=[1, 2],
        hidden_layer_neurons_list=[-1],
        epoch_list=[10],
        batch_size_list=[128, 512],
        learning_rate_list=[0.01, 0.1],
        momentum_list=[0.0, 0.5, 0.8],
        activation_function_list=['sigmoid'],
        kernel_initializer_list=['GlorotNormal'],
        loss_function_list=['mse'],
        reduction_output_neurons_list=[50],
        n_folds=3,
        X_transformed_test=x_test_df,
        n_jobs=6,
        metrics=DEFAULT_METRICS,
    )

    best_activation = 'sigmoid'
    best_reduction_neurons = 3
    best_epoch = 3

    # create model
    autoencoder = create_fit_model(
        X_transformed=x_train_df,
        reduction_output_neurons=best_reduction_neurons,
        hidden_layers=2,
        hidden_layers_neurons=10,
        activation_hidden_function=best_activation,
        output_activation_function='sigmoid',
        learning_rate=0.01,
        momentum=0.0,
        epochs=best_epoch,
        loss=tf.keras.losses.MeanSquaredError(),
        kernel_initializer='GlorotNormal',
        metrics=None,
    )

    get_features(model=autoencoder, input_df=x_test_df, prefix_column='sample_ae')
