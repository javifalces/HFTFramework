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
    "l1": 0.,
    "l2": 0.,
    "isRNN": False,
    "stateColumnsFilter": [],
    "seed": 0,
    "otherInstrumentsStates": [],
    "otherInstrumentsMsPeriods": []
}

PRIVATE_COLUMNS = 2


class AlphaAvellanedaStoikov(DQNAlgorithm):
    NAME = AlgorithmEnum.alpha_avellaneda_stoikov

    def __init__(self, algorithm_info: str, parameters: dict = DEFAULT_PARAMETERS):
        super().__init__(
            algorithm_info=algorithm_info, parameters=parameters
        )
        self.is_filtered_states = False
        if 'stateColumnsFilter' in parameters.keys() and parameters['stateColumnsFilter'] is not None and len(
                parameters['stateColumnsFilter']) > 0:
            self.is_filtered_states = True


    def _get_action_columns(self):
        skew_actions = self.parameters['skewPricePctAction']
        risk_aversion_actions = self.parameters['riskAversionAction']
        windows_actions = self.parameters['windowsTickAction']
        actions = []
        counter = 0
        for action in skew_actions:
            for risk_aversion in risk_aversion_actions:
                for windows_action in windows_actions:
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
            algorithm_enum=AlgorithmEnum.alpha_avellaneda_stoikov
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




if __name__ == '__main__':
    avellaneda_dqn = AlphaAvellanedaStoikov(algorithm_info='test_main_dqn')
    most_significant_state_columns = [
        'ask_price_0',
        'ask_price_1',
        'ask_price_4',
        'ask_price_5',
        'ask_price_7',
        'ask_qty_0',
        'ask_qty_1',
        'ask_qty_2',
        'ask_qty_3',
        'bid_price_0',
        'bid_price_1',
        'bid_price_4',
        'bid_price_5',
        'bid_price_7',
        'bid_price_8',
        'bid_qty_0',
        'last_close_price_6',
        'microprice_0',
        'midprice_0',
        'midprice_3',
        'midprice_9',
        'spread_0',
        'spread_1',
        'spread_4',
        'spread_5',
        'spread_7'
    ]
    parameters_base_pt = DEFAULT_PARAMETERS
    parameters_base_pt['epoch'] = 3
    parameters_base_pt['maxBatchSize'] = 100
    parameters_base_pt['batchSize'] = 12
    parameters_base_pt['stateColumnsFilter'] = most_significant_state_columns
    parameters_base_pt['isRNN'] = False

    avellaneda_dqn.set_parameters(parameters=parameters_base_pt)

    # print('Starting training')
    output_train = avellaneda_dqn.train(
        instrument_pk='btcusdt_binance',
        start_date=datetime.datetime(year=2020, day=8, month=12, hour=10),
        end_date=datetime.datetime(year=2020, day=8, month=12, hour=14),
        iterations=3,
        fill_memory_max_iterations=2,
        simultaneous_algos=2,
        algos_per_iteration=2,
        clean_initial_experience=True,

        # algos_per_iteration=1,
        # simultaneous_algos=1,
    )

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
