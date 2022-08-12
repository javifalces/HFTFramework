import datetime
from typing import Type

from trading_algorithms import dqn_algorithm
from trading_algorithms.market_making import avellaneda_stoikov
from trading_algorithms.market_making.avellaneda_stoikov import SpreadCalculationEnum
from trading_algorithms.dqn_algorithm import DQNAlgorithm, ReinforcementLearningType

from trading_algorithms.algorithm import Algorithm
from trading_algorithms.algorithm_enum import AlgorithmEnum
import pandas as pd

from trading_algorithms.iterations_period_time import IterationsPeriodTime
from backtest.parameter_tuning.ga_configuration import GAConfiguration
from backtest.pnl_utils import get_score
from trading_algorithms.score_enum import ScoreEnum
from configuration import BACKTEST_OUTPUT_PATH


DEFAULT_PARAMETERS = {
    # rl4j-only
    'maxSteps': 10000,
    'maxTrainingIterations': 999999,
    'forceTraining': 0,
    'numLayers': 2,
    'targetTrainSteps': 2500,
    # actions
    'skewPricePctAction': [0.0, 0.05, -0.05, -0.1, 0.1],
    'riskAversionAction': [0.01, 0.1, 0.2, 0.9],
    'windowsTickAction': [24.69],
    "kDefaultAction": [-1],
    "aDefaultAction": [-1],
    # states
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
    # reward
    "scoreEnum": ScoreEnum.asymmetric_dampened_pnl,
    "timeHorizonSeconds": (5),
}
DEFAULT_PARAMETERS.update(dqn_algorithm.DEFAULT_PARAMETERS)
DEFAULT_PARAMETERS.update(avellaneda_stoikov.DEFAULT_PARAMETERS)


class Rl4jAlphaAvellanedaStoikov(DQNAlgorithm):
    NAME = AlgorithmEnum.rl4j_alpha_avellaneda_stoikov

    def __init__(self, algorithm_info: str, parameters: dict = DEFAULT_PARAMETERS):
        parameters = Algorithm.set_defaults_parameters(
            parameters=parameters, DEFAULT_PARAMETERS=DEFAULT_PARAMETERS
        )
        super().__init__(algorithm_info=algorithm_info, parameters=parameters)
        self.is_filtered_states = False
        if (
            'stateColumnsFilter' in parameters.keys()
            and parameters['stateColumnsFilter'] is not None
            and len(parameters['stateColumnsFilter']) > 0
        ):
            self.is_filtered_states = True

    def _get_action_columns(self):
        skew_actions = self.parameters['skewPricePctAction']
        risk_aversion_actions = self.parameters['riskAversionAction']
        windows_actions = self.parameters['windowsTickAction']
        k_default = self.parameters['kDefaultAction']
        a_default = self.parameters['aDefaultAction']
        actions = []
        counter = 0
        for action in skew_actions:
            for risk_aversion in risk_aversion_actions:
                for windows_action in windows_actions:
                    for k_defaul in k_default:
                        for a_defaul in a_default:
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
        algorithm_enum=AlgorithmEnum.rl4j_alpha_avellaneda_stoikov,
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
            clean_initial_generation_experience=clean_initial_generation_experience,
        )

    def plot_params(
        self, raw_trade_pnl_df: pd.DataFrame, figsize=None, title: str = None
    ):
        import seaborn as sns

        sns.set_theme()
        import matplotlib.pyplot as plt

        try:
            if figsize is None:
                figsize = (20, 12)

            df = self.get_trade_df(raw_trade_pnl_df=raw_trade_pnl_df)
            df.set_index('time', inplace=True)

            print('plotting params from %s to %s' % (df.index[0], df.index[-1]))

            skew_change = True
            windows_change = True
            k_change = True
            a_change = True

            if len(df['skew_pct'].fillna(0).diff().fillna(0).unique()) == 0:
                skew_change = False

            # if len(df['windows_tick'].fillna(0).diff().fillna(0).unique()) == 0:
            #     windows_change = False

            plt.close()
            subplot_origin = 710
            nrows = 7
            if skew_change:
                subplot_origin += 100
                nrows += 1

            if windows_change:
                subplot_origin += 100
                nrows += 1

            subplot_origin += 1

            index = 0
            fig, axs = plt.subplots(nrows=nrows, ncols=1, figsize=figsize, sharex=True)
            color = 'black'
            color_mean = 'gray'
            bid_color = 'green'
            ask_color = 'red'

            window = self.PLOT_ROLLING_WINDOW_TICKS
            alpha = 0.9
            lw = 0.5

            ax = axs[index]
            ax.plot(df['risk_aversion'], color=color, lw=lw, alpha=alpha)
            ax.plot(
                df['risk_aversion'].rolling(window=window).mean(),
                color=color_mean,
                lw=lw - 0.1,
                alpha=alpha,
            )
            ax.set_ylabel('risk_aversion')
            ax.grid(axis='y', ls='--', alpha=0.7)

            index += 1
            ax = axs[index]
            ax.plot(df['k_default'], color=color, lw=lw, alpha=alpha)
            ax.plot(
                df['k_default'].rolling(window=window).mean(),
                color=color_mean,
                lw=lw - 0.1,
                alpha=alpha,
            )
            ax.set_ylabel('k_default')
            ax.grid(axis='y', ls='--', alpha=0.7)

            index += 1
            ax = axs[index]
            ax.plot(df['a_default'], color=color, lw=lw, alpha=alpha)
            ax.plot(
                df['a_default'].rolling(window=window).mean(),
                color=color_mean,
                lw=lw - 0.1,
                alpha=alpha,
            )
            ax.set_ylabel('a_default')
            ax.grid(axis='y', ls='--', alpha=0.7)

            if title is not None:
                ax.set_title(title)

            if windows_change:
                index += 1
                ax = axs[index]
                ax.plot(df['windows_tick'], color=color, lw=lw, alpha=alpha)
                ax.plot(
                    df['windows_tick'].rolling(window=window).mean(),
                    color=color_mean,
                    lw=lw - 0.1,
                    alpha=alpha,
                )

                ax.set_ylabel('windows_tick')
                ax.grid(axis='y', ls='--', alpha=0.7)

            if skew_change:
                index += 1
                ax = axs[index]
                ax.plot(df['skew_pct'], color=color, lw=lw, alpha=alpha)
                ax.set_ylabel('skew_pct')
                ax.grid(axis='y', ls='--', alpha=0.7)

            fig = self.plot_params_base(
                fig,
                axs=axs,
                last_index_plotted=index,
                color=color,
                color_mean=color_mean,
                bid_color=bid_color,
                ask_color=ask_color,
                lw=lw,
                alpha=alpha,
                raw_trade_pnl_df=raw_trade_pnl_df,
            )
            plt.show()
            return fig
        except Exception as e:
            print('Some error plotting params %s' % e)
        return None

    def train(
        self,
        start_date: datetime.datetime,
        end_date: datetime,
        instrument_pk: str,
        iterations: int,
        algos_per_iteration: int,
        simultaneous_algos: int = 1,
        score_early_stopping: ScoreEnum = ScoreEnum.realized_pnl,
        patience: int = -1,
        clean_initial_experience: bool = False,
        train_each_iteration: bool = False,
        min_iterations: int = None,
        force_explore_prob: float = None,
        plot_training: bool = True,
        plot_training_iterations: int = 0,
        fill_memory_max_iterations: int = None,
    ) -> list:
        import copy

        parameter_start = copy.copy(self.parameters)
        parameter_train = copy.copy(self.parameters)
        parameter_train['maxTrainingIterations'] = iterations
        if parameter_train['maxSteps'] is None:
            parameter_train['maxSteps'] = 10000
        parameter_train['forceTraining'] = 1
        self.set_parameters(parameter_train)
        output_dict = super().test(
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            explore_prob=1.0,
            clean_experience=True,
        )
        self.set_parameters(parameter_start)  # restore parameters

        return [output_dict]


if __name__ == '__main__':
    skewPricePctAction = [0.0, 1.0, -1.0, -5.0, 5.0]
    trainingPredictIterationPeriod = (
        IterationsPeriodTime.END_OF_SESSION
    )  # OFF END_OF_SESSION
    trainingTargetIterationPeriod = (
        IterationsPeriodTime.END_OF_SESSION
    )  # OFF END_OF_SESSION
    QUANTITY = 0.1
    FIRST_HOUR = 8
    LAST_HOUR = 15

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
        'spread_7',
    ]
    parameters_default_dqn = {
        # Q
        "skewPricePctAction": skewPricePctAction,
        "riskAversionAction": [0.5],
        "kDefaultAction": [0.1],
        "aDefaultAction": [0.1],
        "spreadCalculation": SpreadCalculationEnum.GueantTapia,
        "sigmaDefault": (0.2),
        "windowsTickAction": [5, 6],
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
        "horizonCandlesState": (10),
        "horizonMinMsTick": (5),
        "scoreEnum": ScoreEnum.asymmetric_dampened_pnl,  # ScoreEnum.asymmetric_dampened_pnl,
        "timeHorizonSeconds": (5),
        "epsilon": (0.2),  # probability of explore=> random action
        "discountFactor": 0.75,  # next state prediction reward discount
        "learningRate": 0.85,  # 0.25 in phd? new values reward multiplier
        "momentumNesterov": 0.5,  # momentum nesterov nn
        "learningRateNN": 0.01,  # on nn
        # Avellaneda default
        "riskAversion": (0.5),  # will be override
        "windowTick": (10),  # will be override
        "minutesChangeK": (1),  # will be override
        "quantity": (QUANTITY),
        "positionMultiplier": (1.0),
        "kDefault": (-1),
        "spreadMultiplier": (1.0),
        "firstHour": (FIRST_HOUR),
        "lastHour": (LAST_HOUR),
        # DQN
        "maxBatchSize": 10000,  # 10000
        "batchSize": 1000,  # 5000
        "trainingPredictIterationPeriod": trainingPredictIterationPeriod,  # train only at the end,offline
        "trainingTargetIterationPeriod": trainingTargetIterationPeriod,  # train at the end,offline
        "epoch": 150,  # 150
        "stateColumnsFilter": most_significant_state_columns,
        "l1": 0.0,
        "l2": 0.0,
        "seed": 28220,
        "calculateTt": 0,
        "reinforcementLearningType": ReinforcementLearningType.double_deep_q_learn,
        "parameterTuningBeforeTraining": 1,
        "earlyStoppingTraining": 1,
        "earlyStoppingDataSplitTrainingPct": 0.6,
        "epochMultiplierParameterTuning": [150, 75, 300],
        "learningRateParameterTuning": [0.00001, 0.0001, 0.001, 0.01],
        "hiddenSizeNodesMultiplierParameterTuning": [2.0, 1.0, 0.5],
        "batchSizeParameterTuning": [32, 64, 128],
        "momentumParameterTuning": [0.5, 0.8, 0.0],
        "l1ParameterTuning": [0.0, 0.1, 0.01, 0.001],
        "l2ParameterTuning": [0.0, 0.1, 0.01, 0.001],
    }
    best_avellaneda_param_dict = {
        'riskAversion': 0.079665431,
        'windowTick': 25,
        'minutesChangeK': 1,
        'quantity': QUANTITY,
        'kDefault': 0.001,
        'aDefault': 2.039242,
        'spreadMultiplier': 1.0,
        'positionMultiplier': 1.0,
        'firstHour': FIRST_HOUR,
        'lastHour': LAST_HOUR,
        "seed": 28220,
        "calculateTt": 0,
    }

    algorithm_info_dqn = 'avellaneda_stoikov_dqn_2'

    avellaneda_dqn = Rl4jAlphaAvellanedaStoikov(
        algorithm_info=algorithm_info_dqn, parameters=parameters_default_dqn
    )
    avellaneda_dqn.set_parameters(
        best_avellaneda_param_dict
    )  # same optimization as benchmark

    parameters_base_pt = DEFAULT_PARAMETERS
    parameters_base_pt['epoch'] = 3
    parameters_base_pt['maxBatchSize'] = 100
    parameters_base_pt['batchSize'] = 12
    parameters_base_pt['stateColumnsFilter'] = most_significant_state_columns

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
    while True:

        parameters = avellaneda_dqn.get_parameters(explore_prob=explore_prob)
        avellaneda_dqn.set_parameters(parameters)
        if iterations == 0:
            clean_experience = True
        else:
            clean_experience = False
        print(
            rf"starting training with explore_prob = {avellaneda_dqn.parameters['epsilon']}"
        )
        output_test = avellaneda_dqn.test(
            instrument_pk='btcusdt_binance',
            start_date=datetime.datetime(year=2020, day=9, month=12, hour=7),
            end_date=datetime.datetime(year=2020, day=9, month=12, hour=9),
            trainingPredictIterationPeriod=IterationsPeriodTime.END_OF_SESSION,
            trainingTargetIterationPeriod=IterationsPeriodTime.END_OF_SESSION,
            clean_experience=clean_experience,
        )
        # name_output = avellaneda_dqn.NAME + '_' + avellaneda_dqn.algorithm_info + '_0'
        name_output = avellaneda_dqn.get_test_name(
            name=avellaneda_dqn.NAME, algorithm_number=0
        )
        backtest_df = output_test[name_output]

        score = get_score(
            backtest_df=backtest_df,
            score_enum=ScoreEnum.realized_pnl,
            equity_column_score=ScoreEnum.realized_pnl,
        )

        results.append(backtest_df)
        scores.append(score)

        import matplotlib.pyplot as plt

        avellaneda_dqn.plot_trade_results(
            raw_trade_pnl_df=output_test[name_output], title='test %d' % iterations
        )
        plt.show()

        avellaneda_dqn.plot_params(raw_trade_pnl_df=output_test[name_output])
        plt.show()

        pd.Series(scores).plot()
        plt.title(f'scores evolution {explore_prob} {iterations} ')
        plt.show()
        iterations += 1
        explore_prob -= 0.05
        explore_prob = max(explore_prob, 0.05)
