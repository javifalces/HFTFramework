import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import pandas as pd
from sklearn.model_selection import KFold
import numpy as np
import itertools
from tqdm.contrib.itertools import product


DEFAULT_METRICS = [
    tf.keras.metrics.RootMeanSquaredError(),
    tf.keras.metrics.MeanSquaredError(),
]


def join_by_row(table_1: pd.DataFrame, table_2: pd.DataFrame) -> pd.DataFrame:
    import copy

    # df1 = df1.append(df2)
    # df1 = pd.concat([df1, df2], axis=0)
    assert table_1.shape[1] == table_2.shape[1]
    output_df = copy.copy(table_1)
    output_df = output_df.append(table_2)
    return output_df


class ExecutorRun:
    def __init__(
        self,
        hidden_layer_neurons,
        epoch,
        batch_size,
        learning_rate,
        momentum,
        activation_function,
        output_activation_function,
        kernel_initializer,
        loss_function,
        hidden_layers,
        X_normalized,
        y,
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
        self.output_activation_function = output_activation_function
        self.X_normalized = X_normalized
        self.y = y
        self.n_folds = n_folds
        self.shuffle = shuffle
        self.metrics = metrics

        self.oos_score_1 = None
        self.oos_score_2 = None
        self.finished = False

    def run(self):
        try:
            self.finished = False
            self.oos_score_1, self.oos_score_2 = get_out_of_sample_metrics(
                self.X_normalized,
                self.y,
                self.hidden_layers,
                self.hidden_layer_neurons,
                self.learning_rate,
                self.momentum,
                self.epoch,
                self.batch_size,
                self.n_folds,
                self.shuffle,
                self.loss_function,
                self.kernel_initializer,
                self.activation_function,
                self.metrics,
                self.output_activation_function,
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
                self.kernel_initializer,
                self.activation_function,
                self.output_activation_function,
                self.oos_score_1,
                self.oos_score_2,
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
                'kernel_initializer',
                'activation_function',
                'output_activation_function',
                self.metrics[0].name,
                self.metrics[1].name,
            ],
        )
        return df


def create_feedforward_model(
    input_layer_neurons: int,
    hidden_layer_neurons: int,
    output_layer_neurons: int,
    hidden_layers: int,
    learning_rate: float,
    momentum: float,
    loss_function: str = 'mse',
    kernel_initializer: str = 'GlorotNormal',
    activation_function: str = 'sigmoid',
    metrics: list = DEFAULT_METRICS,
    output_activation_function: str = None,
) -> tf.keras.Model:
    optimizer = keras.optimizers.SGD(learning_rate=learning_rate)
    if momentum > 0:
        optimizer = keras.optimizers.SGD(
            learning_rate=learning_rate, momentum=momentum, nesterov=True
        )

    model = keras.Sequential()
    model.add(layers.Dense(input_layer_neurons, kernel_initializer=kernel_initializer))
    if hidden_layer_neurons is None or hidden_layer_neurons < 0:
        hidden_layer_neurons = (input_layer_neurons + output_layer_neurons) * 2
        print(rf"setting hidden_layer_neurons = {hidden_layer_neurons}")

    for layer in range(hidden_layers):
        model.add(
            layers.Dense(
                hidden_layer_neurons,
                kernel_initializer=kernel_initializer,
                activation=activation_function,
            )
        )

    if output_activation_function is None:
        model.add(layers.Dense(output_layer_neurons))
    else:
        model.add(layers.Dense(output_layer_neurons, activation=activation_function))

    model.compile(
        optimizer=optimizer,
        loss=loss_function,
        metrics=metrics,
    )
    return model


def get_out_of_sample_metrics(
    X_normalized: pd.DataFrame,
    y: pd.DataFrame,
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
    metrics: list = DEFAULT_METRICS,
    output_activation_function: str = None,
) -> (float, float):
    model = create_feedforward_model(
        X_normalized.shape[1],
        hidden_layers,
        y.shape[1],
        hidden_layer_neurons,
        learning_rate,
        momentum,
        loss_function,
        kernel_initializer,
        activation_function,
        metrics,
        output_activation_function,
    )
    kf = KFold(n_splits=n_splits, shuffle=shuffle)

    evaluation_1 = []
    evaluation_2 = []
    for train_index, test_index in kf.split(X_normalized):
        X_train_fold = X_normalized.iloc[train_index]
        X_test_fold = X_normalized.iloc[test_index]

        y_train_fold = y.iloc[train_index]
        y_test_fold = y.iloc[test_index]
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
    X_normalized: pd.DataFrame,
    y: pd.DataFrame,
    hidden_layers_list,
    hidden_layer_neurons_list,
    epoch_list,
    batch_size_list,
    learning_rate_list,
    momentum_list,
    activation_function_list,
    output_activation_function_list,
    kernel_initializer_list,
    loss_function_list,
    n_folds,
    shuffle=True,
    metrics: list = DEFAULT_METRICS,
    n_jobs: int = 1,
    X_normalized_test: pd.DataFrame = None,
    y_test: pd.DataFrame = None,
    take_best: int = 5,
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
        output_activation_function_list,
        kernel_initializer_list,
        loss_function_list,
    )
    len_iterations = len(list(iterative))
    print(rf"{len_iterations} iterations running in parallel on {n_jobs} n_jobs ")

    executors = []
    jobs = []
    for (
        hidden_layers,
        hidden_layer_neurons,
        epoch,
        batch_size,
        learning_rate,
        momentum,
        activation_function,
        output_activation_function,
        kernel_initializer,
        loss_function,
    ) in product(
        hidden_layers_list,
        hidden_layer_neurons_list,
        epoch_list,
        batch_size_list,
        learning_rate_list,
        momentum_list,
        activation_function_list,
        output_activation_function_list,
        kernel_initializer_list,
        loss_function_list,
    ):
        executor_job = ExecutorRun(
            hidden_layers=hidden_layers,
            hidden_layer_neurons=hidden_layer_neurons,
            epoch=epoch,
            batch_size=batch_size,
            learning_rate=learning_rate,
            momentum=momentum,
            activation_function=activation_function,
            output_activation_function=output_activation_function,
            kernel_initializer=kernel_initializer,
            loss_function=loss_function,
            X_normalized=X_normalized,
            y=y,
            n_folds=n_folds,
            shuffle=shuffle,
            metrics=metrics,
        )

        executors.append(executor_job)
        job = {"func": executor_job.run}
        jobs.append(job)

    from factor_investing.util.paralellization_util import process_jobs_joblib

    n_jobs = min(n_jobs, len(jobs))
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

    # finished all
    print(rf"finished all parameter tuning ")
    output_df = None

    for executor in executors:
        if output_df is None:
            output_df = executor.get_df()
        else:
            output_df = join_by_row(output_df, executor.get_df())
    if output_df is None:
        print(rf"something wrong no outpunt df!!")
    # sort it
    output_df = output_df.sort_values(metrics[0].name, ascending=True).reset_index(
        drop=True
    )  # lower mse is worst in Scikit

    ## test
    if X_normalized_test is not None and y_test is not None:
        take_best = min(take_best, len(output_df))
        print(rf'validating best results on  {take_best} test')
        print(output_df.head())

        inputs_validating = []
        evaluation_1 = []
        evaluation_2 = []
        import tqdm

        for row in tqdm.tqdm(range(take_best)):
            hidden_layers = int(output_df.iloc[row]['hidden_layers'])
            hidden_layer_neurons = int(output_df.iloc[row]['hidden_layer_neurons'])
            learning_rate = float(output_df.iloc[row]['learning_rate'])
            momentum = float(output_df.iloc[row]['momentum'])
            epoch = int(output_df.iloc[row]['epoch'])
            batch_size = int(output_df.iloc[row]['batch_size'])
            loss_function = str(output_df.iloc[row]['loss_function'])
            kernel_initializer = str(output_df.iloc[row]['kernel_initializer'])
            output_activation_function = str(
                output_df.iloc[row]['output_activation_function']
            )
            activation_function = str(output_df.iloc[row]['activation_function'])
            training_score_1 = str(output_df.iloc[row][metrics[0].name])
            training_score_2 = str(output_df.iloc[row][metrics[1].name])

            X_train_fold = X_normalized
            X_test_fold = X_normalized_test

            y_train_fold = y
            y_test_fold = y_test

            model = create_feedforward_model(
                X_train_fold.shape[1],
                hidden_layers,
                y.shape[1],
                hidden_layer_neurons,
                learning_rate,
                momentum,
                loss_function,
                kernel_initializer,
                activation_function,
                metrics,
                output_activation_function,
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
                    kernel_initializer,
                    activation_function,
                    output_activation_function,
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
                'kernel_initializer',
                'activation_function',
                'output_activation_function',
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

    return output_df


def old_parameter_tuning(
    X_normalized: pd.DataFrame,
    y: pd.DataFrame,
    hidden_layers_list,
    hidden_layer_neurons_list,
    epoch_list,
    batch_size_list,
    learning_rate_list,
    momentum_list,
    activation_function_list,
    kernel_initializer_list,
    loss_function_list,
    n_folds,
    shuffle=True,
    metrics: list = DEFAULT_METRICS,
) -> pd.DataFrame:
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
    )
    len_iterations = len(list(iterative))
    print(
        rf"we are going to train the nn  {len_iterations} times on {n_folds} n_folds "
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
    ):
        mse_iter, rmse_iter = get_out_of_sample_metrics(
            X_normalized,
            y,
            hidden_layers,
            hidden_layer_neurons,
            learning_rate,
            momentum,
            epoch,
            batch_size,
            n_folds,
            shuffle,
            loss_function,
            kernel_initializer,
            activation_function,
            metrics,
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
    )  # lower mse is worst in scikit
    return OOS_kfold_results


if __name__ == '__main__':
    (x_train, y_train), (x_test, y_test) = keras.datasets.mnist.load_data()
    y_train = pd.DataFrame(y_train)  # 60000x28
    y_test = pd.DataFrame(y_test)  # 60000x28
    x_train_df = pd.DataFrame(x_train[:, :, 1])  # 60000x28
    x_test_df = pd.DataFrame(x_test[:, :, 1])  # 10000x28

    results_df = parameter_tuning(
        X_normalized=x_train_df,
        y=y_train,
        hidden_layers_list=[1],
        hidden_layer_neurons_list=[-1],
        epoch_list=[5],
        batch_size_list=[1],
        learning_rate_list=[0.01, 0.1],
        momentum_list=[0.0, 0.5, 0.8],
        activation_function_list=['sigmoid'],
        kernel_initializer_list=['GlorotNormal'],
        loss_function_list=['mse'],
        n_folds=3,
        X_normalized_test=x_test_df,
        y_test=y_test,
        n_jobs=10,
        output_activation_function_list=[None],
        metrics=DEFAULT_METRICS,
    )
