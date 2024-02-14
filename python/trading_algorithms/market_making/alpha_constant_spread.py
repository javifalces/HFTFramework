import datetime
import copy

from trading_algorithms.market_making.constant_spread import ConstantSpreadParameters
from trading_algorithms.reinforcement_learning import rl_algorithm

from trading_algorithms.algorithm import Algorithm
from trading_algorithms.algorithm_enum import AlgorithmEnum
import pandas as pd

from trading_algorithms.iterations_period_time import IterationsPeriodTime
from backtest.parameter_tuning.ga_configuration import GAConfiguration
from backtest.pnl_utils import get_score
from trading_algorithms.market_making import constant_spread
from trading_algorithms.reinforcement_learning.rl_algorithm import (
    RLAlgorithm,
    ReinforcementLearningActionType,
    RlAlgorithmParameters,
)
from trading_algorithms.score_enum import ScoreEnum
from configuration import LAMBDA_OUTPUT_PATH


class AlphaConstantSpreadParameters:
    level_action = "levelAction"
    skew_level_action = "skewLevelAction"


DEFAULT_PARAMETERS = {
    # Action
    AlphaConstantSpreadParameters.level_action: [1, 2, 3, 4, 5],
    AlphaConstantSpreadParameters.skew_level_action: [0, 1, -1],
    # states
    RlAlgorithmParameters.min_private_state: (-1),
    RlAlgorithmParameters.max_private_state: (-1),
    RlAlgorithmParameters.number_decimals_private_state: (-1),
    RlAlgorithmParameters.horizon_ticks_private_state: (5),
    RlAlgorithmParameters.min_market_state: (-1),
    RlAlgorithmParameters.max_market_state: (-1),
    RlAlgorithmParameters.number_decimals_market_state: (-1),
    RlAlgorithmParameters.horizon_ticks_market_state: (10),
    RlAlgorithmParameters.min_candle_state: (-1),
    RlAlgorithmParameters.max_candle_state: (-1),
    RlAlgorithmParameters.number_decimals_candle_state: (-1),
    RlAlgorithmParameters.state_candles_length: (5),
    RlAlgorithmParameters.horizon_min_ms_tick: (0),
    # rewards
    RlAlgorithmParameters.score: ScoreEnum.asymmetric_dampened_pnl,
    RlAlgorithmParameters.step_seconds: (5),
}
DEFAULT_PARAMETERS.update(rl_algorithm.DEFAULT_PARAMETERS)
DEFAULT_PARAMETERS.update(constant_spread.DEFAULT_PARAMETERS)


class AlphaConstantSpread(RLAlgorithm):
    NAME = AlgorithmEnum.alpha_constant_spread

    def __init__(self, algorithm_info: str, parameters: dict = None):
        if parameters is None:
            parameters = copy.copy(DEFAULT_PARAMETERS)

        parameters = Algorithm.set_defaults_parameters(
            parameters=parameters, DEFAULT_PARAMETERS=DEFAULT_PARAMETERS
        )

        super().__init__(
            algorithm_info=self.NAME + "_" + algorithm_info, parameters=parameters
        )
        self.is_filtered_states = False
        if (
                RlAlgorithmParameters.state_filter in parameters.keys()
                and parameters[RlAlgorithmParameters.state_filter] is not None
                and len(parameters[RlAlgorithmParameters.state_filter]) > 0
        ):
            self.is_filtered_states = True

    def _get_action_columns(self):
        levels = self.parameters[AlphaConstantSpreadParameters.level_action]
        skew_levels = self.parameters[AlphaConstantSpreadParameters.skew_level_action]

        actions = []
        if (
                self.reinforcement_learning_action_type
                == ReinforcementLearningActionType.discrete
        ):
            counter = 0
            for level in set(levels):
                for skew_level in set(skew_levels):
                    actions.append('action_%d_reward' % counter)
                    counter += 1
        else:
            actions = [(min(levels), max(levels)), (min(skew_levels), max(skew_levels))]

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
        ga_configuration: GAConfiguration,
        parameters_base: dict = DEFAULT_PARAMETERS,
        clean_initial_generation_experience: bool = True,
        algorithm_enum=AlgorithmEnum.alpha_constant_spread,
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

            # if df['skewLevel'].fillna(0).diff().fillna(0).sum() == 0:
            #     skew_change = False

            plt.close()
            subplot_origin = 510
            nrows = 5
            if skew_change:
                subplot_origin += 100
                nrows += 1

            subplot_origin += 1
            index = 0
            fig, axs = plt.subplots(nrows=nrows, ncols=1, figsize=figsize, sharex=True)
            color = 'black'
            color_mean = 'gray'
            bid_color = 'green'
            ask_color = 'red'

            window = 25
            alpha = 0.9
            lw = 0.5

            ax = axs[index]
            ax.plot(df[ConstantSpreadParameters.level], color=color, lw=lw, alpha=alpha)
            ax.plot(
                df[ConstantSpreadParameters.level].rolling(window=window).mean(),
                color=color_mean,
                lw=lw - 0.1,
                alpha=alpha,
            )

            ax.set_ylabel(ConstantSpreadParameters.level)
            ax.grid(axis='y', ls='--', alpha=0.7)

            if title is not None:
                ax.set_title(title)

            if skew_change:
                index += 1
                ax = axs[index]
                ax.plot(
                    df[ConstantSpreadParameters.skew_level],
                    color=color,
                    lw=lw,
                    alpha=alpha,
                )
                ax.set_ylabel(ConstantSpreadParameters.skew_level)
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
            # plt.show()
            return fig

        except Exception as e:
            print('Some error plotting params %s' % e)
        return None

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
    alpha_constant_spread.clean_model(output_path=LAMBDA_OUTPUT_PATH)
    iterations = 0
    explore_prob = 1.0
    while True:

        parameters = alpha_constant_spread.get_parameters(explore_prob=explore_prob)
        alpha_constant_spread.set_parameters(parameters)
        if iterations == 0:
            clean_experience = True
        else:
            clean_experience = False
        print(
            rf"starting training with explore_prob = {alpha_constant_spread.parameters['epsilon']}"
        )
        output_test = alpha_constant_spread.test(
            instrument_pk='btcusdt_kraken',
            start_date=datetime.datetime(year=2023, day=13, month=11, hour=7),
            end_date=datetime.datetime(year=2023, day=13, month=11, hour=15),
            trainingPredictIterationPeriod=IterationsPeriodTime.END_OF_SESSION,
            trainingTargetIterationPeriod=IterationsPeriodTime.END_OF_SESSION,
            clean_experience=clean_experience,
        )
        # name_output = avellaneda_dqn.NAME + '_' + avellaneda_dqn.algorithm_info + '_0'
        name_output = alpha_constant_spread.get_test_name(
            name=alpha_constant_spread.NAME, algorithm_number=0
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

        alpha_constant_spread.plot_trade_results(
            raw_trade_pnl_df=output_test[name_output], title='test %d' % iterations
        )
        plt.show()

        alpha_constant_spread.plot_params(raw_trade_pnl_df=output_test[name_output])
        plt.show()

        pd.Series(scores).plot()
        plt.title(f'scores evolution {explore_prob} {iterations} ')
        plt.show()
        iterations += 1
        explore_prob -= 0.05
        explore_prob = max(explore_prob, 0.05)
