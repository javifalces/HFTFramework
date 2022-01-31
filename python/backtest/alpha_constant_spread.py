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
    'levelAction': [1, 2, 3, 4, 0.1],
    'skewLevelAction': [0.0, 1, -1],

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
    "l1": 0.,
    "l2": 0.,
    "isRNN": False,
    "stateColumnsFilter": [],
    "seed": 0
}

PRIVATE_COLUMNS = 2


class AlphaConstantSpread(DQNAlgorithm):
    NAME = AlgorithmEnum.alpha_constant_spread

    def __init__(self, algorithm_info: str, parameters: dict = DEFAULT_PARAMETERS):
        super().__init__(
            algorithm_info=algorithm_info, parameters=parameters
        )
        self.is_filtered_states = False
        if 'stateColumnsFilter' in parameters.keys() and parameters['stateColumnsFilter'] is not None and len(
                parameters['stateColumnsFilter']) > 0:
            self.is_filtered_states = True



    def _get_action_columns(self):
        levels = self.parameters['levelAction']
        skew_levels = self.parameters['skewLevelAction']

        actions = []
        counter = 0
        for level in levels:
            for skew_level in skew_levels:
                actions.append('action_%d_reward' % counter)
                counter += 1
        return actions



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
            clean_initial_generation_experience: bool = True,
            algorithm_enum=AlgorithmEnum.alpha_constant_spread
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

    # def get_memory_replay_filename(self, algorithm_number: int = None):
    #     # memoryReplay_DQNRSISideQuoting_eurusd_darwinex_test.csv
    #     return rf"memoryReplay_{self.get_test_name(name=self.NAME, algorithm_number=algorithm_number)}.csv"


if __name__ == '__main__':
    alpha_constant_spread = AlphaConstantSpread(algorithm_info='test_main_dqn')

    parameters_base_pt = DEFAULT_PARAMETERS
    parameters_base_pt['epoch'] = 30
    parameters_base_pt['maxBatchSize'] = 5000
    parameters_base_pt['batchSize'] = 128
    parameters_base_pt['stateColumnsFilter'] = []
    parameters_base_pt['isRNN'] = False

    alpha_constant_spread.set_parameters(parameters=parameters_base_pt)

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
    alpha_constant_spread.clean_model(output_path=BACKTEST_OUTPUT_PATH)
    iterations = 0
    explore_prob = 1.0
    while (True):

        parameters = alpha_constant_spread.get_parameters(explore_prob=explore_prob)
        alpha_constant_spread.set_parameters(parameters)
        if iterations == 0:
            clean_experience = True
        else:
            clean_experience = False
        print(rf"starting training with explore_prob = {alpha_constant_spread.parameters['epsilon']}")
        output_test = alpha_constant_spread.test(
            instrument_pk='btcusdt_binance',
            start_date=datetime.datetime(year=2020, day=9, month=12, hour=7),
            end_date=datetime.datetime(year=2020, day=9, month=12, hour=9),
            trainingPredictIterationPeriod=IterationsPeriodTime.END_OF_SESSION,
            trainingTargetIterationPeriod=IterationsPeriodTime.END_OF_SESSION,
            clean_experience=clean_experience
        )
        # name_output = avellaneda_dqn.NAME + '_' + avellaneda_dqn.algorithm_info + '_0'
        name_output = alpha_constant_spread.get_test_name(name=alpha_constant_spread.NAME, algorithm_number=0)
        backtest_df = output_test[name_output]

        score = get_score(backtest_df=backtest_df, score_enum=ScoreEnum.realized_pnl,
                          equity_column_score=ScoreEnum.realized_pnl)

        results.append(backtest_df)
        scores.append(score)

        import matplotlib.pyplot as plt

        alpha_constant_spread.plot_trade_results(raw_trade_pnl_df=output_test[name_output],
                                                 title='test %d' % iterations)
        plt.show()

        alpha_constant_spread.plot_params(raw_trade_pnl_df=output_test[name_output])
        plt.show()

        pd.Series(scores).plot()
        plt.title(f'scores evolution {explore_prob} {iterations} ')
        plt.show()
        iterations += 1
        explore_prob -= 0.05
        explore_prob = max(explore_prob, 0.05)
