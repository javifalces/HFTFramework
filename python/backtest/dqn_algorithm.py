from backtest.algorithm import Algorithm
import copy
import pandas as pd
import os
from numpy import genfromtxt, savetxt
import glob

from backtest.iterations_period_time import IterationsPeriodTime
from backtest.score_enum import ScoreEnum, get_score_enum_csv_column
import datetime
from backtest.pnl_utils import *
from backtest.input_configuration import (
    BacktestConfiguration,
    AlgorithmConfiguration,
    InputConfiguration,
    JAR_PATH,
    TrainInputConfiguration)
from backtest.backtest_launcher import BacktestLauncher, BacktestLauncherController
import shutil
from backtest.train_launcher import clean_gpu_memory
from configuration import LAMBDA_OUTPUT_PATH

DEFAULT_PARAMETERS = {
    # DQN parameters
    "maxBatchSize": 1000,
    "trainingPredictIterationPeriod": IterationsPeriodTime.END_OF_SESSION,  # train only at the end,offline
    "trainingTargetIterationPeriod": IterationsPeriodTime.END_OF_SESSION,  # train at the end,offline
    "epoch": 75,
    "momentumNesterov": 0.9,  # speed to change learning rate
    "learningRateNN": 0.1,  # between 0.01 to 0.1

    "horizonMinMsTick": (0),
    "scoreEnum": ScoreEnum.total_pnl,

    "epsilon": (0.2),  # probability of explore=> random action
    "discountFactor": 0.95,  # next state prediction reward discount
    "learningRate": 0.95,  # 0.25 in phd? new values reward multiplier

    "quantity": (0.0001),
    "first_hour": (7),
    "last_hour": (19),
    "l1":0.,
    "l2":0.,
    "stateColumnsFilter": []
}

class StateType:
    market_state = "MARKET_STATE"
    ta_state = "TA_STATE"

DEFAULT_TA_INDICATORS_PERIODS = [3, 5, 7, 9, 11, 13, 15, 17, 21]
DEFAULT_MARKET_HORIZON_SAVE = 15
'''
TAKE CARE dont mess up the order with java!!
'''


def ga_ta_state_columns(periods=DEFAULT_TA_INDICATORS_PERIODS,
                        market_horizon_save: int = DEFAULT_MARKET_HORIZON_SAVE) -> list:
    single_state_columns = ["hour_of_the_day_utc", "minutes_from_start", "volume_from_start"]
    ta_prefixes = [
        "microprice",
        "vpin",
        "rsi",
        "sma",
        "ema",
        "max",
        "min",

        "volume_rsi_",
        "volume_sma_",
        "volume_ema_",
        "volume_max_",
        "volume_min_",

    ]
    market_ta_prefixes = [
        "bid_price_", "ask_price_", "bid_qty_", "ask_qty_", "spread_", "imbalance_", "microprice_"
    ]
    states = []
    # candles
    ta_periods = periods
    for ta_prefix in ta_prefixes:
        for period in ta_periods:
            column_name = f"{ta_prefix}_{period}"
            states.append(column_name)

    # market
    for market_prefix in market_ta_prefixes:
        for market_horizon in range(market_horizon_save):
            column_name = f"{market_prefix}_{market_horizon}"
            states.append(column_name)

    return states + single_state_columns


class DQNAlgorithm(Algorithm):
    class TrainType:
        default = "standard"
        standart = "standard"
        custom_actor_critic = "custom_actor_critic"

    REMOVE_PRIVATE_STATES = False  # same as in java to get same states!
    DISABLE_LAST_CLOSE = False
    PRIVATE_COLUMNS = 2
    NAME = 'GENERIC_DQN_ALGORITH'
    DEFAULT_TRAIN_TYPE = TrainType.standart

    state_type = StateType.market_state

    def __init__(self, algorithm_info: str, parameters: dict) -> None:
        super().__init__(
            algorithm_info=algorithm_info, parameters=parameters
        )
        self.train_type = DQNAlgorithm.DEFAULT_TRAIN_TYPE
        self.is_filtered_states = False
        if 'stateColumnsFilter' in parameters.keys() and parameters['stateColumnsFilter'] is not None and len(
                parameters['stateColumnsFilter']) > 0:
            self.is_filtered_states = True

    def get_parameters(self, explore_prob: float) -> dict:
        parameters = copy.copy(self.parameters)
        explore_prob = max(explore_prob, 0.05)
        parameters['epsilon'] = explore_prob
        return parameters

    def get_max_batch_size(self):
        return self.parameters["maxBatchSize"]

    def _get_default_state_columns(self):
        if self.state_type == StateType.market_state:
            return self._get_market_state_columns()

        if self.state_type == StateType.ta_state:
            return self._get_ta_state_columns()

    def _get_ta_state_columns(self):
        return ga_ta_state_columns()

    def _get_market_state_columns(self):
        MARKET_MIDPRICE_RELATIVE = True

        private_states = []
        market__depth_states = []
        candle_states = []
        market__trade_states = []
        private_horizon_ticks = self.parameters['horizonTicksPrivateState']
        market_horizon_ticks = self.parameters['horizonTicksMarketState']
        candle_horizon = self.parameters['horizonCandlesState']

        if not self.REMOVE_PRIVATE_STATES:
            for private_state_horizon in range(private_horizon_ticks - 1, -1, -1):
                private_states.append('private_inventory_%d' % private_state_horizon)

            for private_state_horizon in range(private_horizon_ticks - 1, -1, -1):
                private_states.append('private_score_%d' % private_state_horizon)

        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_bid_price_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_ask_price_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_bid_qty_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_ask_qty_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_spread_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_midprice_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_imbalance_%d' % market_state_horizon)
        for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
            market__depth_states.append('market_microprice_%d' % market_state_horizon)

        if not self.DISABLE_LAST_CLOSE:
            for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
                market__trade_states.append('market_last_close_price_%d' % market_state_horizon)
            for market_state_horizon in range(market_horizon_ticks - 1, -1, -1):
                market__trade_states.append('market_last_close_qty_%d' % market_state_horizon)

        if not MARKET_MIDPRICE_RELATIVE:
            for candle_state_horizon in range(candle_horizon - 1, -1, -1):
                candle_states.append('candle_open_%d' % candle_state_horizon)
        for candle_state_horizon in range(candle_horizon - 1, -1, -1):
            candle_states.append('candle_high_%d' % candle_state_horizon)
        for candle_state_horizon in range(candle_horizon - 1, -1, -1):
            candle_states.append('candle_low_%d' % candle_state_horizon)
        for candle_state_horizon in range(candle_horizon - 1, -1, -1):
            candle_states.append('candle_close_%d' % candle_state_horizon)

        candle_states.append('candle_ma')
        candle_states.append('candle_std')
        candle_states.append('candle_max')
        candle_states.append('candle_min')

        columns_states = private_states + market__depth_states + market__trade_states + candle_states
        return columns_states

    def _get_action_columns(self):
        raise NotImplementedError

    def get_memory_replay_df(self, memory_replay_file: str = None, state_columns: list = None) -> pd.DataFrame:
        if memory_replay_file is None:
            memory_replay_file = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename()
        print(rf"getting memory from {memory_replay_file}")

        if state_columns is None:
            if self.is_filtered_states:
                if self.state_type == StateType.market_state:
                    # add private
                    private_horizon_ticks = self.parameters['horizonTicksPrivateState']
                    state_columns_temp = []
                    if not self.REMOVE_PRIVATE_STATES:
                        for private_state_horizon in range(private_horizon_ticks - 1, -1, -1):
                            state_columns_temp.append('inventory_%d' % private_state_horizon)
                        for private_state_horizon in range(private_horizon_ticks - 1, -1, -1):
                            state_columns_temp.append('score_%d' % private_state_horizon)
                    state_columns_temp += self.parameters['stateColumnsFilter']
                    state_columns = state_columns_temp
                    # sort_ordered=self._get_default_state_columns()

                    # Sort columns in the right order
                    # state_columns=[]
                    # for state_column in sort_ordered:
                    #     if state_column in state_columns_temp:
                    #         state_columns.append(state_column)

            else:
                state_columns = self._get_default_state_columns()

        action_columns = self._get_action_columns()
        next_state_actions = copy.copy(state_columns)
        next_state_actions = ['next_' + state for state in next_state_actions]

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
                f'my_data.shape[1] {my_data.shape[1]}!= {len(all_columns)} len(all_columns)  are the parameters correct as in the file?')
            return None

        output = pd.DataFrame(my_data, columns=all_columns)
        return output

    def get_number_of_state_columns(self, parameters: dict) -> int:
        state_columns = []
        if 'stateColumnsFilter' in list(parameters.keys()):
            state_columns = parameters['stateColumnsFilter']
            for state_str in copy.copy(state_columns):
                # remove private not filtered! to add it later
                if 'score' in state_str or 'inventory' in state_str:
                    del state_columns[state_str]

        if state_columns is None or len(state_columns) == 0:
            number_state_columns = len(self._get_default_state_columns())
            # number_state_columns = parameters['horizonTicksPrivateState'] * PRIVATE_COLUMNS + parameters[
            #     'horizonTicksMarketState'] * MARKET_COLUMNS + parameters[
            #                            'horizonCandlesState'] * CANDLE_COLUMNS + CANDLE_INDICATOR_COLUMNS
        else:
            # add private columns
            number_state_columns = len(state_columns)
            if self.state_type == StateType.market_state:
                number_state_columns += (parameters['horizonTicksPrivateState'] * self.PRIVATE_COLUMNS)

        return number_state_columns

    def get_number_of_action_columns(self, parameters: dict) -> int:
        raise NotImplementedError

    def merge_q_matrix(self, backtest_launchers: list, algos_per_iteration: int = None,
                       include_original: bool = False) -> list:
        import numpy as np
        # base_path_search = backtest_launchers[0].output_path
        base_path_search = LAMBDA_OUTPUT_PATH
        csv_files = glob.glob(LAMBDA_OUTPUT_PATH + os.sep + '*.csv')

        algorithm_names = []

        for backtest_launcher in backtest_launchers:
            algorithm_names.append(backtest_launcher.id)

        csv_files_out = []
        for csv_file in csv_files:
            if 'permutation' in csv_file:
                continue

            for algorithm_name in algorithm_names:
                if self._is_memory_file(csv_file, algorithm_name=algorithm_name):
                    if algos_per_iteration is not None:
                        number_algo = self.get_iteration_number_filename(csv_file)
                        if number_algo > algos_per_iteration:
                            continue

                    csv_files_out.append(csv_file)
        if include_original:
            csv_files_out.append(LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename())

        csv_files_out = list(set(csv_files_out))
        print(
            'combining %d memory_replay for %d launchers from %s'
            % (len(csv_files_out), len(backtest_launchers), base_path_search)
        )
        # assert len(csv_files_out) == len(backtest_launchers)
        output_array = None
        for csv_file_out in csv_files_out:
            try:
                my_data = genfromtxt(csv_file_out, delimiter=',')
                print("combining %d rows from %s" % (len(my_data), csv_file_out))
                if len(my_data) == 0 or len(my_data[0][:]) == 0:
                    print('has no valid data ,ignore %s' % csv_file_out)
                    continue

            except Exception as e:
                print('error loading memory %s-> skip it  %s' % (csv_file_out, e.args))
                continue
            my_data = my_data[:, :]  # remove last column of nan
            if output_array is None:
                output_array = my_data
            else:
                output_array = np.append(output_array.T, my_data.T, axis=1).T
        if output_array is None:
            print('cant combine %d files or no data t combine at %s!' % (len(csv_files_out), base_path_search))
            return
        df = pd.DataFrame(output_array)

        number_state_columns = self.get_number_of_state_columns(self.parameters)
        number_of_actions = self.get_number_of_action_columns(parameters=self.parameters)

        df_state_columns = list(df.columns)[:number_state_columns]
        df_next_state_columns = list(df.columns)[-number_state_columns:]
        df_reward_columns = list(df.columns)[number_state_columns:-number_state_columns]
        # check everyting is fine!
        try:
            assert len(df_reward_columns) == number_of_actions
            assert len(df_state_columns) == number_state_columns
            assert len(df_next_state_columns) == number_state_columns
        except Exception as e:
            print(
                rf"something goes wrong when number_of_actions={number_of_actions}!={len(df_reward_columns)} on memory or and number_state_columns={number_state_columns}!= {len(df_state_columns)} on memory ")
            print(e)
            raise e
        # change rewards==0 to nan
        df[df_reward_columns] = df[df_reward_columns].replace(0, np.nan)

        # group states discarting nan
        df = df.groupby(df_state_columns, dropna=True).max()  # combine in the same state rows!
        df.fillna(0, inplace=True)

        max_batch_size = self.parameters['maxBatchSize']

        # add the q matrix for normal algo name
        csv_files_out.append(LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename())
        print("saving %d rows in %d files" % (len(df), len(csv_files_out)))

        # Override it randomize
        for csv_file_out in csv_files_out:
            if len(df) > max_batch_size:
                df = df.sample(max_batch_size)
            # shuffle to save merge -> lost of time sequence
            output_array = df.sample(frac=1).reset_index().values

            savetxt(csv_file_out, output_array, delimiter=',', fmt='%.18e')

        return csv_files_out

    def get_memory_replay_filename(self, algorithm_number: int = None):
        # memoryReplay_DQNRSISideQuoting_eurusd_darwinex_test.csv
        return rf"memoryReplay_{self.get_test_name(name=self.NAME, algorithm_number=algorithm_number)}.csv"

    def get_predict_model_filename(self, algorithm_number: int = None):
        # predict_model_DQNRSISideQuoting_eurusd_darwinex.model
        return rf"predict_model_{self.get_test_name(name=self.NAME, algorithm_number=algorithm_number)}.model"

    def get_target_model_filename(self, algorithm_number: int = None):
        # target_model_DQNRSISideQuoting_eurusd_darwinex.model
        return rf"target_model_{self.get_test_name(name=self.NAME, algorithm_number=algorithm_number)}.model"

    def get_memory_size(self, memory_replay_file: str = None, algorithm_number: int = None) -> int:
        memory_df = self.get_memory_replay_df(memory_replay_file=memory_replay_file)
        if memory_df is None:
            return 0
        return len(memory_df)

    def fill_memory(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            algos_per_iteration: int,
            simultaneous_algos: int = 1,
            clean_initial_experience: bool = False,
            max_iterations=50,
    ):

        backtest_configuration = BacktestConfiguration(
            start_date=start_date, end_date=end_date, instrument_pk=instrument_pk
        )
        explore_prob = 1.0  # i dont care
        max_batch_size = self.get_max_batch_size()
        clean_gpu_memory()
        iteration = 0
        if not clean_initial_experience and self.get_memory_size() >= self.get_max_batch_size():
            print(rf"already filled memory  {self.get_memory_size()}/{self.get_max_batch_size()} rows")
            return

        while (True):
            if iteration > max_iterations:
                print(f"iteration {iteration}>{max_iterations} max_iterations")
                return
            print("------------------------")
            print("fill_memory iteration %d memorySize %d/%d" % (
                iteration, self.get_memory_size(), self.get_max_batch_size()))
            print("------------------------")
            backtest_launchers = []
            is_finished = False
            algorithm_name = self.algorithm_info
            for algorithm_number in range(algos_per_iteration):
                parameters = self.get_parameters(
                    explore_prob=explore_prob
                )

                self.set_training_seed(parameters=parameters, iteration=iteration, algorithm_number=algorithm_number)
                if algos_per_iteration > 1:
                    algorithm_name = '%s_%s_%d' % (
                        self.NAME,
                        self.algorithm_info,
                        algorithm_number,
                    )

                if simultaneous_algos > 1:
                    parameters['trainingPredictIterationPeriod'] = IterationsPeriodTime.OFF
                    parameters['trainingTargetIterationPeriod'] = IterationsPeriodTime.OFF

                print('fill_memory on algorithm %s  explore_prob:%.2f' % (algorithm_name, explore_prob))
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
            ## all backtest launchers are ready
            # clean before
            if clean_initial_experience and iteration == 0 and os.path.isdir(LAMBDA_OUTPUT_PATH):
                # clean it
                print('cleaning experience on training  path %s' % LAMBDA_OUTPUT_PATH)
                self.clean_experience(output_path=LAMBDA_OUTPUT_PATH)
                memory_file_path = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename()
                if os.path.isfile(memory_file_path):
                    os.remove(memory_file_path)

                print('cleaning models on training  path %s' % LAMBDA_OUTPUT_PATH)
                self.clean_model(output_path=LAMBDA_OUTPUT_PATH)
            # in case number of states/actions changes
            self.clean_permutation_cache(output_path=LAMBDA_OUTPUT_PATH)

            # Launch it
            backtest_controller = BacktestLauncherController(
                backtest_launchers=backtest_launchers,
                max_simultaneous=simultaneous_algos,
            )
            output_dict = backtest_controller.run()
            # Combine experience
            if algos_per_iteration > 1:
                memory_files = self.merge_q_matrix(backtest_launchers=backtest_launchers,
                                                   algos_per_iteration=algos_per_iteration, include_original=True)
                if memory_files is None:
                    print(f'something was wrong on merge_q_matrix -> memory files is None')
                else:
                    # copying into orignial name memoryReplay
                    memory_replay_out = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename()
                    if memory_files[0] != memory_replay_out:
                        print(
                            'copying  %s memory_replay to  %s'
                            % (memory_files[0], memory_replay_out)
                        )
                        shutil.copy(memory_files[0], memory_replay_out)
            # check size of experience
            memory_size = self.get_memory_size()
            if memory_size < self.get_max_batch_size():
                print(
                    rf"memory_df small {memory_size}<max_batch_size {self.get_max_batch_size()} =>iteration again")
                iteration += 1
            else:
                memory_backup = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename() + ".backup"
                memory_replay_out = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename()
                shutil.copy(memory_replay_out, memory_backup)
                print(f"fill_memory finished with {memory_size} rows -> backup memory on {memory_backup}")
                return

    def train(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            iterations: int,
            algos_per_iteration: int,
            simultaneous_algos: int = 1,
            score_early_stopping: ScoreEnum = ScoreEnum.falcma_ratio,  # in case of iterations<0
            patience: int = -1,
            clean_initial_experience: bool = False,
            train_each_iteration: bool = True,
            min_iterations: int = None,
            force_explore_prob: float = None,
            fill_memory_before: bool = True

    ) -> list:
        max_batch_size = self.parameters['maxBatchSize']
        memory_size = self.get_memory_size()
        if fill_memory_before and (clean_initial_experience or memory_size < max_batch_size):
            self.fill_memory(start_date=start_date, end_date=end_date, instrument_pk=instrument_pk,
                             algos_per_iteration=algos_per_iteration,
                             simultaneous_algos=simultaneous_algos,
                             clean_initial_experience=clean_initial_experience
                             )
            print("finished fill memory=> set training to one worker only")
            algos_per_iteration = 1
            simultaneous_algos = 1
            clean_initial_experience = False

        backtest_configuration = BacktestConfiguration(
            start_date=start_date, end_date=end_date, instrument_pk=instrument_pk
        )
        explore_prob = 1.0  # i dont care
        if force_explore_prob is not None:
            explore_prob = force_explore_prob

        momentum_nesterov = self.parameters['momentumNesterov']
        learning_rate = self.parameters['learningRateNN']
        number_epochs = self.parameters['epoch']
        l1 = self.parameters['l1']
        l2 = self.parameters['l2']

        if 'batchSize' in self.parameters.keys():
            batch_size = self.parameters['batchSize']
        else:
            batch_size = max_batch_size / 10
            batch_size = max(batch_size, 512)

        output_list = []
        memory_files = []
        range_iterations = iterations
        if iterations < 0 and patience >= 0:
            print('no finish until early stopping')
            range_iterations = 2000

        if (iterations <= 0 and patience < 0):
            print('no finish until early stopping with no patience => set patience to 1')
            patience = 1

        prev_best_score = None
        patience_counter = 0
        keep_same_iteration = False
        iterations_discount = 0

        for iteration in range(range_iterations):

            clean_gpu_memory()
            print("------------------------")
            print("Training iteration %d/%d" % (iteration, range_iterations - 1))
            print("------------------------")

            backtest_launchers = []
            is_finished = False
            algorithm_name = self.algorithm_info
            for algorithm_number in range(algos_per_iteration):
                if not keep_same_iteration:
                    if (iterations <= 0):
                        explore_prob = 1 - iterations_discount / 10
                        if explore_prob < 0.1:
                            explore_prob = 0.1 - (iterations_discount - 10) / 100
                    else:
                        explore_prob = 1 - iterations_discount / iterations
                    #maintain
                    if force_explore_prob is not None:
                        explore_prob=force_explore_prob

                    keep_same_iteration = False
                if iterations <= 0 and explore_prob <= 0.01:
                    print("limit number of iterations reached on early stopping explore_prob<=0.01-> break")
                    is_finished = True
                    break
                explore_prob = max(0.01, explore_prob)
                parameters = self.get_parameters(
                    explore_prob=explore_prob
                )

                self.set_training_seed(parameters=parameters, iteration=iteration, algorithm_number=algorithm_number)

                algorithm_name = self.get_test_name(name=self.NAME)
                if algos_per_iteration > 1:
                    algorithm_name = self.get_test_name(name=self.NAME, algorithm_number=algorithm_number)

                parameters['trainingPredictIterationPeriod'] = IterationsPeriodTime.OFF
                parameters['trainingTargetIterationPeriod'] = IterationsPeriodTime.OFF

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
            if is_finished:
                break
            if clean_initial_experience and iteration == 0 and os.path.isdir(backtest_launchers[0].output_path):
                # clean it
                # print('cleaning experience on training  path %s' % backtest_launchers[0].output_path)
                # self.clean_experience(output_path=backtest_launchers[0].output_path)
                # memory_file_path = LAMBDA_OUTPUT_PATH+os.sep+self.get_memory_replay_filename()
                # if os.path.isfile(memory_file_path):
                #     os.remove(memory_file_path)

                print('cleaning models on training  path %s' % LAMBDA_OUTPUT_PATH)
                self.clean_model(output_path=LAMBDA_OUTPUT_PATH)

            if not clean_initial_experience and iteration == 0:
                # copy original into different instances
                original_file = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename()
                print(f'copy {original_file}  on {algos_per_iteration} algos')
                for algos_it in range(algos_per_iteration):
                    target_file = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename(
                        algorithm_number=algos_it + 1)
                    shutil.copy(original_file, target_file)

            # in case number of states/actions changes
            self.clean_permutation_cache(output_path=LAMBDA_OUTPUT_PATH)

            # Launch it
            backtest_controller = BacktestLauncherController(
                backtest_launchers=backtest_launchers,
                max_simultaneous=simultaneous_algos,
            )
            output_dict = backtest_controller.run()
            output_list.append(output_dict)
            # Combine experience
            memory_files = self.merge_q_matrix(backtest_launchers=backtest_launchers,
                                               algos_per_iteration=algos_per_iteration,
                                               include_original=True
                                               )
            if memory_files is None:
                print(f'something was wrong on merge_q_matrix -> memory files is None')
                memory_files = []
            else:
                # copying into orignial name memoryReplay
                memory_replay_out = LAMBDA_OUTPUT_PATH + os.sep + self.get_memory_replay_filename()
                if memory_replay_out != memory_files[0]:
                    print(
                        'copying  %s memory_replay to  %s'
                        % (memory_files[0], memory_replay_out)
                    )
                    shutil.copy(memory_files[0], memory_replay_out)

            if algos_per_iteration > 1:
                predict_models = []
                target_models = []
                for memory_file in memory_files:
                    predict_models.append(
                        memory_file.replace('memoryReplay', 'predict_model').replace('.csv', '.model'))
                    target_models.append(memory_file.replace('memoryReplay', 'target_model').replace('.csv', '.model'))
                predict_models.append(LAMBDA_OUTPUT_PATH + os.sep + self.get_predict_model_filename())
                target_models.append(LAMBDA_OUTPUT_PATH + os.sep + self.get_target_model_filename())
            else:
                predict_models = [LAMBDA_OUTPUT_PATH + os.sep + self.get_predict_model_filename()]
                target_models = [LAMBDA_OUTPUT_PATH + os.sep + self.get_target_model_filename()]

            ### get best score of algos
            best_score = -9999.
            scores_iteration = []
            for algo_key in output_dict.keys():
                try:
                    backtest_df = output_dict[algo_key]
                    score = get_score(backtest_df=backtest_df, score_enum=score_early_stopping,
                                      equity_column_score=ScoreEnum.realized_pnl)
                    if np.isfinite(score):
                        scores_iteration.append(score)
                        equity_curve = backtest_df[get_score_enum_csv_column(ScoreEnum.realized_pnl)]
                        returns = equity_curve.pct_change(periods=1).replace([0.0, np.nan, np.inf, -np.inf],
                                                                             np.nan).dropna()
                        max_dd = get_max_drawdown_pct(equity_curve)
                        print(
                            f"score {score_early_stopping} on {algo_key} = {score} [trades={len(equity_curve)} mean_returns={returns.mean()} med_returns={returns.median()} std_returns={returns.std()} max_dd_pct={max_dd}]")
                    else:
                        print(
                            f"{algo_key} algo with not finite score! {score} len backtest_df={len(backtest_df)} -> skip it")
                except Exception as e:
                    print(rf"error getting score of {algo_key} {str(e)}->skip it")

            best_score = np.median(scores_iteration)

            if "useAsQLearn" in self.parameters.keys() and self.parameters["useAsQLearn"]==1:
                print("useAsQLearn detected=> not train")
                train_each_iteration=False#dont need to train anything

            # train nn
            if train_each_iteration and len(memory_files) > 0:
                memory_file = memory_files[0]
                predict_model = predict_models[0]
                target_model = target_models[0]

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
                                                                    momentum_nesterov=momentum_nesterov,
                                                                    max_batch_size=max_batch_size,
                                                                    batch_size=batch_size,
                                                                    train_type=self.train_type
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
                    if self.train_type == DQNAlgorithm.DEFAULT_TRAIN_TYPE:
                        for target_model_it in target_models:
                            print(f'copying from predict model {predict_model} to target {target_model_it}')
                            shutil.copy(predict_model, target_model_it)

                if not self.train_type == DQNAlgorithm.DEFAULT_TRAIN_TYPE and os.path.exists(target_model):
                    for target_model_it in target_models:
                        if target_model == target_model_it:
                            print(f'not copying target model {target_model} in {target_model_it} , is the same')
                            continue
                        print(f'copying target model {target_model} in {target_model_it}')
                        shutil.copy(target_model, target_model_it)


            else:
                if (len(memory_files)):
                    print(f"can't train if no data available or no train each period-> next")

            ### check patience
            print("-------------\n\n")
            print(f'best score {score_early_stopping} on iteration {iteration} is {best_score}')
            if prev_best_score is not None:
                if best_score <= prev_best_score:
                    patience_counter += 1
                    keep_same_iteration = True  # this explore_prob will not count
                    print(
                        f'prev_best_score {prev_best_score} >={best_score} current best scores -> patience_counter++ keep_same_iteration {patience_counter}/{patience}')
                else:
                    patience_counter = 0
                    iterations_discount += 1
                    keep_same_iteration = False
                    print(
                        f'best_score {best_score} >{prev_best_score} prev_best_score -> {patience_counter}/{patience}')
                    prev_best_score = best_score
            else:
                prev_best_score = best_score
                iterations_discount += 1
            print("-------------\n\n")
            is_min_iterations_done = True
            if min_iterations is not None and iteration < min_iterations:
                # restart until iterations>=min_iteartions
                patience_counter = 0

            if patience >= 0 and patience_counter > patience:
                print(f'early stopping reached on iteration {iteration} with best score {prev_best_score}')
                break

        clean_gpu_memory()
        print(rf"one last iteration training")
        parameters = self.get_parameters(explore_prob=explore_prob)
        last_train_dict = self.test(start_date=start_date, end_date=end_date, instrument_pk=instrument_pk,
                                    explore_prob=explore_prob,
                                    trainingPredictIterationPeriod=parameters['trainingPredictIterationPeriod'],
                                    trainingTargetIterationPeriod=parameters['trainingTargetIterationPeriod'],
                                    clean_experience=False
                                    )
        output_list.append(last_train_dict)

        return output_list

    def test(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            explore_prob: float = 0.2,
            trainingPredictIterationPeriod: int = None,  # -1 offline train at the end
            trainingTargetIterationPeriod: int = None,  # -1 offline train at the end
            algorithm_number: int = None,
            clean_experience: bool = False,
    ) -> dict:

        backtest_configuration = BacktestConfiguration(
            start_date=start_date, end_date=end_date, instrument_pk=instrument_pk
        )
        parameters = self.get_parameters(explore_prob=explore_prob)
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

    def set_parameters(self, parameters: dict):
        super().set_parameters(parameters)
        if 'stateColumnsFilter' in parameters.keys() and parameters['stateColumnsFilter'] is not None and len(
                parameters['stateColumnsFilter']) > 0:
            self.is_filtered_states = True
