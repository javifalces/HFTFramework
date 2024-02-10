# import unittest
#
# import pandas as pd
# import numpy as np
# import copy
#
# from sklearn.model_selection import train_test_split
# from sklearn.preprocessing import StandardScaler
# from trading_algorithms.trend_predictor.trend_predictor import TrendPredictor
# import os
#
# from utils.tensorflow_utils.parameter_tuning_utils import *
#
#
# @unittest.skip("skip,we dont have the data")
# class ParameterTuningUtilsTest(unittest.TestCase):
#     parameter_tuning_test_size = 0.33
#
#     parameter_tuning_max_rows = 200
#     parameter_tuning_max_features = 10
#     parameter_tuning_max_outputs = 3
#
#     seed_number = 0
#
#     full_path = os.path.realpath(__file__)
#     file_path = os.path.dirname(full_path)
#     data_path= rf"{file_path}\..\..\..\data"
#     input_data_df = pd.read_csv(rf"{data_path}\input_data_df.csv", index_col=0)
#     buy_target_data_df = pd.read_csv(rf"{data_path}\buy_target_data_df.csv", index_col=0)
#     sell_target_data_df = pd.read_csv(rf"{data_path}\sell_target_data_df.csv", index_col=0)
#     print(rf"input_data_df:{input_data_df.shape}")
#     print(rf"buy_target_data_df:{buy_target_data_df.shape}")
#     print(rf"sell_target_data_df:{sell_target_data_df.shape}")
#
#     def parameter_tuning_test(self, X, y, n_folds, n_jobs):
#         test_size = self.parameter_tuning_test_size
#         random_state = self.seed_number
#
#         X_train, X_test, y_buy_train, y_buy_test = train_test_split(X, y, test_size=test_size,
#                                                                     random_state=random_state)
#
#         scaler = StandardScaler()
#         X_train_normalized = scaler.fit_transform(X_train)
#         X_test_normalized = scaler.transform(X_test)
#
#         X_normalized = pd.DataFrame(X_train_normalized)
#         X_normalized_test = pd.DataFrame(X_test_normalized)
#
#         y_buy = pd.DataFrame(y_buy_train)
#         y_buy_test = pd.DataFrame(y_buy_test)
#
#         hidden_neurons = (X_train.shape[1] + y_buy_train.shape[1])
#
#         hidden_layer_neurons_list = [int(hidden_neurons / 2)]
#         epoch_list = [100, 50]
#         hidden_layers_list = [1]
#         batch_size_list = [32]
#         learning_rate_list = [0.1]
#         momentum_list = [0.0]
#
#         parameter_tuning_results_df_buy = parameter_tuning(
#             X_normalized=X_normalized,
#             y=y_buy,
#             hidden_layers_list=hidden_layers_list,
#             hidden_layer_neurons_list=hidden_layer_neurons_list,
#             epoch_list=epoch_list,
#             batch_size_list=batch_size_list,
#             learning_rate_list=learning_rate_list,
#             momentum_list=momentum_list,
#
#             activation_function_list=[TrendPredictor.ACTIVATION_FUNCTION],
#             kernel_initializer_list=[TrendPredictor.INITIALIZER_KERNEL],
#             loss_function_list=[TrendPredictor.LOSS_FUNCTION],
#             n_folds=n_folds,
#             X_normalized_test=X_normalized_test,
#             y_test=y_buy_test,
#             n_jobs=n_jobs,
#             output_activation_function_list=[TrendPredictor.OUTPUT_ACTIVATION_FUNCTION],
#             metrics=DEFAULT_METRICS_CLASSIFICATION,
#         )
#         return parameter_tuning_results_df_buy
#
#     def test_singleprocess(self):
#         n_folds = 3
#         n_jobs = 1
#
#         if self.parameter_tuning_max_rows < 0:
#             X = copy.copy(self.input_data_df).values
#             y_buy = copy.copy(self.buy_target_data_df).values
#             y_sell = copy.copy(self.sell_target_data_df).values
#         else:
#             X = copy.copy(
#                 self.input_data_df.iloc[:self.parameter_tuning_max_rows, :self.parameter_tuning_max_features]).values
#             y_buy = copy.copy(self.buy_target_data_df.iloc[:self.parameter_tuning_max_rows,
#                               :self.parameter_tuning_max_outputs]).values
#             y_sell = copy.copy(self.sell_target_data_df.iloc[:self.parameter_tuning_max_rows,
#                                :self.parameter_tuning_max_outputs]).values
#
#         parameter_tuning_results_df_buy = self.parameter_tuning_test(X, y_buy, n_folds, n_jobs)
#         assert parameter_tuning_results_df_buy is not None
#         assert parameter_tuning_results_df_buy.shape[0] == 2
#
#     def test_multipleprocess(self):
#         n_folds = 3
#         n_jobs = 2
#
#         if self.parameter_tuning_max_rows < 0:
#             X = copy.copy(self.input_data_df).values
#             y_buy = copy.copy(self.buy_target_data_df).values
#             y_sell = copy.copy(self.sell_target_data_df).values
#         else:
#             X = copy.copy(
#                 self.input_data_df.iloc[:self.parameter_tuning_max_rows, :self.parameter_tuning_max_features]).values
#             y_buy = copy.copy(self.buy_target_data_df.iloc[:self.parameter_tuning_max_rows,
#                               :self.parameter_tuning_max_outputs]).values
#             y_sell = copy.copy(self.sell_target_data_df.iloc[:self.parameter_tuning_max_rows,
#                                :self.parameter_tuning_max_outputs]).values
#
#         parameter_tuning_results_df_buy = self.parameter_tuning_test(X, y_buy, n_folds, n_jobs)
#         assert parameter_tuning_results_df_buy is not None
#         assert parameter_tuning_results_df_buy.shape[0] == 2
