import datetime
from typing import Type
import copy
import shutil
import numpy as np
import time

from backtest.dqn_algorithm import DQNAlgorithm
from numpy import genfromtxt, savetxt

from backtest.algorithm import Algorithm
from backtest.algorithm_enum import AlgorithmEnum
from backtest.backtest_launcher import BacktestLauncher, BacktestLauncherController
from backtest.input_configuration import (
    BacktestConfiguration,
    AlgorithmConfiguration,
    InputConfiguration,
    JAR_PATH,
    TrainInputConfiguration)
import glob
import os
import pandas as pd

from backtest.iterations_period_time import IterationsPeriodTime
from backtest.parameter_tuning.ga_configuration import GAConfiguration
from backtest.pnl_utils import get_score
from backtest.score_enum import ScoreEnum
from backtest.train_launcher import clean_gpu_memory
from configuration import LAMBDA_OUTPUT_PATH, BACKTEST_OUTPUT_PATH

DEFAULT_PARAMETERS = {
    # DQN parameters
    "maxBatchSize": 10000,
    "batchSize": 500,
    "trainingPredictIterationPeriod": IterationsPeriodTime.END_OF_SESSION,  # train only at the end,offline
    "trainingTargetIterationPeriod": IterationsPeriodTime.END_OF_SESSION,  # train at the end,offline
    "epoch": 75,
    # Q
    'skewPricePctAction': [0.0, 0.05, -0.05, -0.1, 0.1],
    'riskAversionAction': [0.01, 0.1, 0.2, 0.9],
    'windowsTickAction': [24.69],


    "minPrivateState": (-1),
    "maxPrivateState": (-1),
    "numberDecimalsPrivateState": (3),
    "horizonTicksPrivateState": (5),

    "minMarketState": (-1),
    "maxMarketState": (-1),
    "numberDecimalsMarketState": (7),
    "horizonTicksMarketState": (10),

    "minCandleState": (-1),
    "maxCandleState": (-1),
    "numberDecimalsCandleState": (3),
    "horizonCandlesState": (2),

    "horizonMinMsTick": (0),
    "scoreEnum": ScoreEnum.asymmetric_dampened_pnl,
    "timeHorizonSeconds": (5),

    "epsilon": (0.2),  # probability of explore=> random action

    "discountFactor": 0.75,  # next state prediction reward discount
    "learningRate": 0.85,  # 0.25 in phd? new values reward multiplier
    "momentumNesterov": 0.0,  # # speed to change learning rate
    "learningRateNN": 0.0001,  # # between 0.01 to 0.1

    # Avellaneda default
    "risk_aversion": (0.6),
    "position_multiplier": (335),
    "calculateTt": 1,  # if 1 reserve price effect will be less affected by position with the session time
    "window_tick": (25),
    "minutes_change_k": (1),
    "quantity": (0.0001),  # 1/quantity
    "k_default": (-1),
    "spread_multiplier": (5.0),
    "first_hour": (7),
    "last_hour": (19),
    "l1":0.,
    "l2":0.,
    "isRNN":False,
    "stateColumnsFilter": []
}

PRIVATE_COLUMNS = 2


class AvellanedaDQN(DQNAlgorithm):
    NAME = AlgorithmEnum.avellaneda_dqn

    def __init__(self, algorithm_info: str, parameters: dict = DEFAULT_PARAMETERS):
        super().__init__(
            algorithm_info=algorithm_info, parameters=parameters
        )
        self.is_filtered_states = False
        if 'stateColumnsFilter' in parameters.keys() and parameters['stateColumnsFilter'] is not None and len(
                parameters['stateColumnsFilter']) > 0:
            self.is_filtered_states = True



    def get_parameters(self, explore_prob: float) -> dict:
        parameters = copy.copy(self.parameters)
        parameters['epsilon'] = explore_prob
        return parameters

    def _get_default_state_columns(self):
        MARKET_MIDPRICE_RELATIVE=True

        private_states=[]
        market__depth_states=[]
        candle_states=[]
        market__trade_states=[]
        private_horizon_ticks=self.parameters['horizonTicksPrivateState']
        market_horizon_ticks=self.parameters['horizonTicksMarketState']
        candle_horizon=self.parameters['horizonCandlesState']

        for private_state_horizon in range(private_horizon_ticks-1,-1,-1):
            private_states.append('inventory_%d'%private_state_horizon)

        for private_state_horizon in range(private_horizon_ticks-1,-1,-1):
            private_states.append('score_%d'%private_state_horizon)

        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__depth_states.append('bid_price_%d'%market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__depth_states.append('ask_price_%d'%market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__depth_states.append('bid_qty_%d'%market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__depth_states.append('ask_qty_%d'%market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__depth_states.append('spread_%d'%market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__depth_states.append('midprice_%d'%market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__depth_states.append('imbalance_%d'%market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__depth_states.append('microprice_%d'%market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__trade_states.append('last_close_price_%d'%market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks-1,-1,-1):
            market__trade_states.append('last_close_qty_%d'%market_state_horizon)


        if not MARKET_MIDPRICE_RELATIVE:
            for candle_state_horizon in range(candle_horizon-1,-1,-1):
                candle_states.append('open_%d'%candle_state_horizon)
        for candle_state_horizon in range(candle_horizon-1,-1,-1):
            candle_states.append('high_%d'%candle_state_horizon)
        for candle_state_horizon in range(candle_horizon-1,-1,-1):
            candle_states.append('low_%d'%candle_state_horizon)
        for candle_state_horizon in range(candle_horizon-1,-1,-1):
            candle_states.append('close_%d'%candle_state_horizon)

        candle_states.append('ma')
        candle_states.append('std')
        candle_states.append('max')
        candle_states.append('min')

        columns_states=private_states+market__depth_states+market__trade_states+candle_states
        return columns_states

    def _get_action_columns(self):
        skew_actions=self.parameters['skewPricePctAction']
        risk_aversion_actions=self.parameters['riskAversionAction']
        windows_actions=self.parameters['windowsTickAction']
        actions = []
        counter = 0
        for action in skew_actions:
            for risk_aversion in risk_aversion_actions:
                for windows_action in windows_actions:
                    actions.append('action_%d_reward' % counter)
                    counter += 1
        return actions

    def get_memory_replay_df(self, memory_replay_file: str = None, state_columns: list = None) -> pd.DataFrame:
        if memory_replay_file is None:
            memory_replay_file = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename()

        if state_columns is None:
            if self.is_filtered_states:
                # add private
                private_horizon_ticks = self.parameters['horizonTicksPrivateState']
                state_columns_temp = []
                for private_state_horizon in range(private_horizon_ticks - 1, -1, -1):
                    state_columns_temp.append('inventory_%d' % private_state_horizon)
                for private_state_horizon in range(private_horizon_ticks - 1, -1, -1):
                    state_columns_temp.append('score_%d' % private_state_horizon)
                state_columns_temp += self.parameters['stateColumnsFilter']
                state_columns=state_columns_temp
                # sort_ordered=self._get_default_state_columns()

                #Sort columns in the right order
                # state_columns=[]
                # for state_column in sort_ordered:
                #     if state_column in state_columns_temp:
                #         state_columns.append(state_column)

            else:
                state_columns=self._get_default_state_columns()

        action_columns = self._get_action_columns()
        next_state_actions = copy.copy(state_columns)

        all_columns = state_columns + action_columns + next_state_actions

        ## read memory
        if not os.path.exists(memory_replay_file):
            print('file not found %s' % memory_replay_file)
            return None
        my_data = genfromtxt(memory_replay_file, delimiter=',')
        last_column_is_all_None = len(my_data[:, -1][np.logical_not(np.isnan(my_data[:, -1]))]) == 0
        if last_column_is_all_None:
            my_data = my_data[:, :-1]  # last column is reading as all nulls

        try:
            assert my_data.shape[1] == len(all_columns)
        except Exception as e:
            print(
                rf"cant get_memory_replay_df because my_data columns {my_data.shape[1]}!= {len(all_columns)} all_columns are the parameters correct as in the file? ")
            return None
        output = pd.DataFrame(my_data, columns=all_columns)

        return output



    def train(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            iterations: int,
            algos_per_iteration: int,
            simultaneous_algos: int = 1,
            clean_initial_experience: bool = False,
    ) -> list:

        backtest_configuration = BacktestConfiguration(
            start_date=start_date, end_date=end_date, instrument_pk=instrument_pk
        )
        momentum_nesterov = self.parameters['momentumNesterov']
        learning_rate = self.parameters['learningRateNN']
        number_epochs = self.parameters['epoch']
        l1 = self.parameters['l1']
        l2 = self.parameters['l2']
        is_rnn=False
        if 'isRNN' in self.parameters.keys():
            is_rnn=self.parameters['isRNN']


        max_batch_size = self.parameters['maxBatchSize']
        if 'batchSize' in self.parameters.keys():
            batch_size = self.parameters['batchSize']
        else:
            batch_size = max_batch_size / 10
            batch_size = max(batch_size, 512)

        output_list = []
        for iteration in range(iterations):
            print("------------------------")
            if is_rnn:
                print("Training RNN iteration %d/%d" % (iteration, iterations - 1))
            else:
                print("Training iteration %d/%d" % (iteration, iterations - 1))
            print("------------------------")

            backtest_launchers = []
            for algorithm_number in range(algos_per_iteration):

                explore_prob =1 - iteration / iterations

                parameters = self.get_parameters(
                    explore_prob=explore_prob
                )
                self.set_training_seed(parameters=parameters, iteration=iteration, algorithm_number=algorithm_number)

                algorithm_name = '%s_%s_%d' % (
                    self.NAME,
                    self.algorithm_info,
                    algorithm_number,
                )
                print('training on algorithm %s  explore_prob:%.2f' % (algorithm_name, explore_prob))
                algorithm_configurationQ = AlgorithmConfiguration(
                    algorithm_name=algorithm_name, parameters=parameters
                )
                input_configuration = InputConfiguration(
                    backtest_configuration=backtest_configuration,
                    algorithm_configuration=algorithm_configurationQ,
                )

                backtest_launcher = BacktestLauncher(
                    input_configuration=input_configuration,
                    id=algorithm_name,
                    jar_path=JAR_PATH,
                )
                backtest_launchers.append(backtest_launcher)

            if iteration == 0 and clean_initial_experience and os.path.isdir(backtest_launchers[0].output_path):
                # clean it
                print('cleaning experience on training  path %s' % backtest_launchers[0].output_path)
                self.clean_experience(output_path=backtest_launchers[0].output_path)
                print('cleaning models on training  path %s' % backtest_launchers[0].output_path)
                self.clean_model(output_path=backtest_launchers[0].output_path)

            if not clean_initial_experience and iteration == 0:
                # copy original into different instances
                original_file = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename()
                print(f'copy {original_file}  on {algos_per_iteration} algos')
                for algos_it in range(algos_per_iteration):
                    target_file = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename(
                        algorithm_number=algos_it + 1)
                    shutil.copy(original_file, target_file)

            # in case number of states/actions changes
            self.clean_permutation_cache(output_path=backtest_launchers[0].output_path)

            # Launch it
            backtest_controller = BacktestLauncherController(
                backtest_launchers=backtest_launchers,
                max_simultaneous=simultaneous_algos,
            )
            output_dict = backtest_controller.run()
            output_list.append(output_dict)
            # Combine experience
            memory_files = self.merge_q_matrix(backtest_launchers=backtest_launchers)
            if memory_files is None:
                print(f'something was wrong on merge_q_matrix -> memory files is None')
                memory_files = []

            predict_models = []
            target_models = []
            for memory_file in memory_files:
                predict_models.append(memory_file.replace('memoryReplay', 'predict_model').replace('.csv', '.model'))
                target_models.append(memory_file.replace('memoryReplay', 'target_model').replace('.csv', '.model'))

            # train nn
            memory_file = memory_files[0]
            predict_model = predict_models[0]
            # target_model = target_models[0]

            state_columns = self.get_number_of_state_columns(self.parameters)
            action_columns = self.get_number_of_action_columns(self.parameters)
            train_input_configuration = TrainInputConfiguration(memory_path=memory_file,
                                                                output_model_path=predict_model,
                                                                state_columns=state_columns,
                                                                action_columns=action_columns,
                                                                number_epochs=number_epochs,
                                                                learning_rate=learning_rate,
                                                                l1=l1,
                                                                l2=l2,
                                                                batch_size=batch_size,
                                                                max_batch_size=max_batch_size,
                                                                momentum_nesterov=momentum_nesterov,
                                                                is_rnn=is_rnn
                                                                )

            print(f'training {predict_model} on {memory_file}')
            self.train_model(jar_path=JAR_PATH, train_input_configuration=train_input_configuration)
            # copy to all  for next iteration have a trained nn
            if os.path.exists(predict_model):
                for predict_model_it in predict_models:
                    if predict_model == predict_model_it:
                        print(f'not copying predict model {predict_model} in {predict_model_it} , is the same')
                        continue
                    print(f'copying predict model {predict_model} in {predict_model_it}')
                    shutil.copy(predict_model, predict_model_it)
                for target_model_it in target_models:
                    print(f'copying from predict model {predict_model} to target {target_model_it}')
                    shutil.copy(predict_model, target_model_it)

        return output_list

    def test(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            explore_prob: float = None,
            trainingPredictIterationPeriod: int = None,  # -1 offline train at the end
            trainingTargetIterationPeriod: int = None,  # -1 offline train at the end
            algorithm_number: int = 0,
            clean_experience: bool = False,
    ) -> dict:
        backtest_configuration = BacktestConfiguration(
            start_date=start_date, end_date=end_date, instrument_pk=instrument_pk
        )

        if explore_prob is not None:
            parameters = self.get_parameters(explore_prob=explore_prob)
        else:
            parameters = self.get_parameters(explore_prob=self.parameters['epsilon'])

        if trainingPredictIterationPeriod is not None:
            parameters['trainingPredictIterationPeriod'] = trainingPredictIterationPeriod

        if trainingTargetIterationPeriod is not None:
            parameters['trainingTargetIterationPeriod'] = trainingTargetIterationPeriod

        algorithm_name = self.get_test_name(name=self.NAME, algorithm_number=algorithm_number)
        print('testing on algorithm %s' % algorithm_name)
        algorithm_configurationQ = AlgorithmConfiguration(
            algorithm_name=algorithm_name, parameters=parameters
        )
        input_configuration = InputConfiguration(
            backtest_configuration=backtest_configuration,
            algorithm_configuration=algorithm_configurationQ,
        )

        backtest_launcher = BacktestLauncher(
            input_configuration=input_configuration,
            id=algorithm_name,
            jar_path=JAR_PATH,
        )

        if clean_experience:
            print('cleaning experience on test on path %s' % backtest_launcher.output_path)
            self.clean_experience(output_path=backtest_launcher.output_path)
            print('cleaning models on training  path %s' % backtest_launcher.output_path)
            self.clean_model(output_path=backtest_launcher.output_path)

        backtest_controller = BacktestLauncherController(
            backtest_launchers=[backtest_launcher], max_simultaneous=1
        )
        output_dict = backtest_controller.run()

        if (output_dict is None or len(output_dict) == 0):
            print("not output generated in java! something was wrong")

        # output_dict[self.algorithm_info] = output_dict[algorithm_name]
        # del output_dict[algorithm_name]

        return output_dict

    def get_number_of_state_columns(self, parameters: dict) -> int:
        state_columns=[]
        if 'stateColumnsFilter' in list(parameters.keys()):
            state_columns = parameters['stateColumnsFilter']
            for state_str in copy.copy(state_columns):
                # remove private not filtered! to add it later
                if 'score' in state_str or 'inventory' in state_str:
                    del state_columns[state_str]


        if state_columns is None or len(state_columns) == 0:
            number_state_columns = len(self._get_default_state_columns())
        else:
            #add private columns
            number_state_columns = len(state_columns)
            number_state_columns+=(parameters['horizonTicksPrivateState'] * PRIVATE_COLUMNS)

        return number_state_columns

    def get_number_of_action_columns(self, parameters: dict) -> int:
        number_of_lists = 0
        list_of_lists=[]
        for parameter_key in parameters.keys():
            value = parameters[parameter_key]
            if isinstance(value, list) and parameter_key != 'stateColumnsFilter':
                number_of_lists += 1
                list_of_lists.append(value)
        assert number_of_lists==3
        number_of_actions=0
        for first_list in list_of_lists[0]:
            for second_list in list_of_lists[1]:
                for third_list in list_of_lists[2]:
                    number_of_actions+=1
        return number_of_actions

    def parameter_tuning(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            parameters_min: dict,
            parameters_max: dict,
            max_simultaneous: int,
            generations: int,
            ga_configuration: Type[GAConfiguration],
            parameters_base: dict = DEFAULT_PARAMETERS,
            clean_initial_generation_experience:bool=True,
            algorithm_enum=AlgorithmEnum.avellaneda_dqn
    ) -> (dict, pd.DataFrame):

        return super().parameter_tuning(
            algorithm_enum=algorithm_enum,
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            parameters_base=parameters_base,
            parameters_min=parameters_min,
            parameters_max=parameters_max,
            max_simultaneous=max_simultaneous,
            generations=generations,
            ga_configuration=ga_configuration,
            clean_initial_generation_experience=clean_initial_generation_experience
        )




    def set_parameters(self, parameters: dict):
        super().set_parameters(parameters)
        if 'stateColumnsFilter' in parameters.keys() and parameters['stateColumnsFilter'] is not None and len(
                parameters['stateColumnsFilter']) > 0:
            self.is_filtered_states = True

    def get_memory_replay_filename(self, algorithm_number: int = None):
        # memoryReplay_DQNRSISideQuoting_eurusd_darwinex_test.csv
        return rf"memoryReplay_{self.get_test_name(name=self.NAME, algorithm_number=algorithm_number)}.csv"


if __name__ == '__main__':
    avellaneda_dqn = AvellanedaDQN(algorithm_info='test_main_dqn')

    parameters_base_pt = DEFAULT_PARAMETERS
    parameters_base_pt['epoch'] = 30
    parameters_base_pt['maxBatchSize'] = 5000
    parameters_base_pt['batchSize'] = 128
    parameters_base_pt['stateColumnsFilter'] = []
    parameters_base_pt['isRNN'] = False

    avellaneda_dqn.set_parameters(parameters=parameters_base_pt)

    # print('Starting training')
    # output_train = avellaneda_dqn.train(
    #     instrument_pk='btcusdt_binance',
    #     start_date=datetime.datetime(year=2020, day=8, month=12, hour=10),
    #     end_date=datetime.datetime(year=2020, day=8, month=12, hour=14),
    #     iterations=3,
    #     # algos_per_iteration=1,
    #     # simultaneous_algos=1,
    # )

    # name_output = avellaneda_dqn.NAME + '_' + avellaneda_dqn.algorithm_info + '_0'

    # backtest_result_train = output_train[0][name_output]
    # # memory_replay_file = r'E:\Usuario\Coding\Python\market_making_fw\python_lambda\output\memoryReplay_AvellanedaDQN_test_main_dqn_0.csv'
    # # memory_replay_df=avellaneda_dqn.get_memory_replay_df(memory_replay_file=memory_replay_file)
    #
    # avellaneda_dqn.plot_trade_results(raw_trade_pnl_df=backtest_result_train,title='train initial')
    #
    # backtest_result_train = output_train[-1][name_output]
    # avellaneda_dqn.plot_trade_results(raw_trade_pnl_df=backtest_result_train,title='train final')

    print('Starting testing')

    results = []
    scores = []
    avellaneda_dqn.clean_model(output_path=BACKTEST_OUTPUT_PATH)
    iterations = 0
    explore_prob = 1.0
    while (True):

        parameters = avellaneda_dqn.get_parameters(explore_prob=explore_prob)
        avellaneda_dqn.set_parameters(parameters)
        if iterations == 0:
            clean_experience = True
        else:
            clean_experience = False
        print(rf"starting training with explore_prob = {avellaneda_dqn.parameters['epsilon']}")
        output_test = avellaneda_dqn.test(
            instrument_pk='btcusdt_binance',
            start_date=datetime.datetime(year=2020, day=9, month=12, hour=7),
            end_date=datetime.datetime(year=2020, day=9, month=12, hour=9),
            trainingPredictIterationPeriod=IterationsPeriodTime.END_OF_SESSION,
            trainingTargetIterationPeriod=IterationsPeriodTime.END_OF_SESSION,
            clean_experience=clean_experience
        )
        # name_output = avellaneda_dqn.NAME + '_' + avellaneda_dqn.algorithm_info + '_0'
        name_output = avellaneda_dqn.get_test_name(name=avellaneda_dqn.NAME, algorithm_number=0)
        backtest_df = output_test[name_output]

        score = get_score(backtest_df=backtest_df, score_enum=ScoreEnum.realized_pnl,
                          equity_column_score=ScoreEnum.realized_pnl)

        results.append(backtest_df)
        scores.append(score)

        import matplotlib.pyplot as plt

        avellaneda_dqn.plot_trade_results(raw_trade_pnl_df=output_test[name_output], title='test %d' % iterations)
        plt.show()

        avellaneda_dqn.plot_params(raw_trade_pnl_df=output_test[name_output])
        plt.show()

        pd.Series(scores).plot()
        plt.title(f'scores evolution {explore_prob} {iterations} ')
        plt.show()
        iterations += 1
        explore_prob -= 0.05
        explore_prob = max(explore_prob, 0.05)
