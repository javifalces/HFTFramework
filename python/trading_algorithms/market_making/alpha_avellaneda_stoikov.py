import datetime
import copy
from backtest.input_configuration import MultiThreadConfiguration
from trading_algorithms.reinforcement_learning import rl_algorithm
from trading_algorithms.market_making import avellaneda_stoikov
from trading_algorithms.market_making.avellaneda_stoikov import (
    AvellanedaStoikovParameters,
)

from trading_algorithms.algorithm import Algorithm, AlgorithmParameters
from trading_algorithms.algorithm_enum import AlgorithmEnum
import pandas as pd

from backtest.parameter_tuning.ga_configuration import GAConfiguration
from backtest.pnl_utils import get_score
from trading_algorithms.reinforcement_learning.rl_algorithm import (
    RLAlgorithm,
    ReinforcementLearningActionType,
    BaseModelType,
    RlAlgorithmParameters,
)
from trading_algorithms.reinforcement_learning.core.baselines3.core_baselines3_callbacks import (
    InfoStepKey,
)
from trading_algorithms.score_enum import ScoreEnum


class AlphaAvellanedaAlgorithmParameters:
    skew_action = 'skewAction'
    risk_aversion_action = 'riskAversionAction'
    midprice_period_window_action = 'midpricePeriodWindowAction'
    change_k_period_seconds_action = 'changeKPeriodSecondsAction'
    k_default_action = 'kDefaultAction'
    a_default_action = 'aDefaultAction'


DEFAULT_PARAMETERS = {
    # actions
    # applied in levels limited by best levels worst levels in the order book.
    # quoted bid is going to be between worst_bid best_bid
    AlphaAvellanedaAlgorithmParameters.skew_action: [0, 1, -1, -2, 2],
    AlphaAvellanedaAlgorithmParameters.risk_aversion_action: [0.01, 0.1, 0.2, 0.9],
    AlphaAvellanedaAlgorithmParameters.midprice_period_window_action: [10],
    AlphaAvellanedaAlgorithmParameters.change_k_period_seconds_action: [60],
    AlphaAvellanedaAlgorithmParameters.k_default_action: [-1],
    AlphaAvellanedaAlgorithmParameters.a_default_action: [-1],
    # states
    RlAlgorithmParameters.min_private_state: -1,
    RlAlgorithmParameters.max_private_state: -1,
    RlAlgorithmParameters.number_decimals_private_state: -1,
    RlAlgorithmParameters.horizon_ticks_private_state: 5,
    RlAlgorithmParameters.min_market_state: -1,
    RlAlgorithmParameters.max_market_state: -1,
    RlAlgorithmParameters.number_decimals_market_state: -1,
    RlAlgorithmParameters.horizon_ticks_market_state: 10,
    RlAlgorithmParameters.min_candle_state: -1,
    RlAlgorithmParameters.max_candle_state: -1,
    RlAlgorithmParameters.number_decimals_candle_state: -1,
    RlAlgorithmParameters.horizon_candles_state: 2,
    RlAlgorithmParameters.horizon_min_ms_tick: 0,
    # reward
    RlAlgorithmParameters.score_enum: ScoreEnum.asymmetric_dampened_pnl,
    RlAlgorithmParameters.step_seconds: 5,
}
DEFAULT_PARAMETERS.update(rl_algorithm.DEFAULT_PARAMETERS)
DEFAULT_PARAMETERS.update(avellaneda_stoikov.DEFAULT_PARAMETERS)


class AlphaAvellanedaStoikov(RLAlgorithm):
    NAME = AlgorithmEnum.alpha_avellaneda_stoikov

    def __init__(self, algorithm_info: str, parameters=None):
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
        skew_actions = self.parameters[AlphaAvellanedaAlgorithmParameters.skew_action]
        risk_aversion_actions = self.parameters[
            AlphaAvellanedaAlgorithmParameters.risk_aversion_action
        ]
        windows_actions = self.parameters[
            AlphaAvellanedaAlgorithmParameters.midprice_period_window_action
        ]
        k_default = self.parameters[AlphaAvellanedaAlgorithmParameters.k_default_action]
        a_default = self.parameters[AlphaAvellanedaAlgorithmParameters.a_default_action]
        change_k_period_seconds = self.parameters[
            AlphaAvellanedaAlgorithmParameters.change_k_period_seconds_action
        ]
        actions = []
        if (
                self.reinforcement_learning_action_type
                == ReinforcementLearningActionType.discrete
        ):
            counter = 0
            for action in set(skew_actions):
                for risk_aversion in set(risk_aversion_actions):
                    for windows_action in set(windows_actions):
                        for k_defaul in set(k_default):
                            for a_defaul in set(a_default):
                                for k_period_seconds in set(change_k_period_seconds):
                                    actions.append('action_%d_reward' % counter)
                                    counter += 1
        else:
            actions = [
                (min(windows_actions), max(windows_actions)),
                (min(risk_aversion_actions), max(risk_aversion_actions)),
                (min(skew_actions), max(skew_actions)),
                (min(k_default), max(k_default)),
                (min(a_default), max(a_default)),
                (min(change_k_period_seconds), max(change_k_period_seconds)),
            ]

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
            parameters_base=None,
            clean_initial_generation_experience: bool = True,
            algorithm_enum=AlgorithmEnum.alpha_avellaneda_stoikov,
    ) -> (dict, pd.DataFrame):

        if parameters_base is None:
            parameters_base = DEFAULT_PARAMETERS
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

            if (
                    len(
                        df[AvellanedaStoikovParameters.skew]
                                .fillna(0)
                                .diff()
                                .fillna(0)
                                .unique()
                    )
                    == 0
            ):
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
            ax.plot(
                df[AvellanedaStoikovParameters.risk_aversion],
                color=color,
                lw=lw,
                alpha=alpha,
            )
            ax.plot(
                df[AvellanedaStoikovParameters.risk_aversion]
                .rolling(window=window)
                .mean(),
                color=color_mean,
                lw=lw - 0.1,
                alpha=alpha,
            )
            ax.set_ylabel(AvellanedaStoikovParameters.risk_aversion)
            ax.grid(axis='y', ls='--', alpha=0.7)

            if AvellanedaStoikovParameters.k_default in df.columns:
                index += 1
                ax = axs[index]
                ax.plot(
                    df[AvellanedaStoikovParameters.k_default],
                    color=color,
                    lw=lw,
                    alpha=alpha,
                )
                ax.plot(
                    df[AvellanedaStoikovParameters.k_default]
                    .rolling(window=window)
                    .mean(),
                    color=color_mean,
                    lw=lw - 0.1,
                    alpha=alpha,
                )
                ax.set_ylabel(AvellanedaStoikovParameters.k_default)
                ax.grid(axis='y', ls='--', alpha=0.7)

            if AvellanedaStoikovParameters.a_default in df.columns:
                index += 1
                ax = axs[index]
                ax.plot(
                    df[AvellanedaStoikovParameters.a_default],
                    color=color,
                    lw=lw,
                    alpha=alpha,
                )
                ax.plot(
                    df[AvellanedaStoikovParameters.a_default]
                    .rolling(window=window)
                    .mean(),
                    color=color_mean,
                    lw=lw - 0.1,
                    alpha=alpha,
                )
                ax.set_ylabel(AvellanedaStoikovParameters.a_default)
                ax.grid(axis='y', ls='--', alpha=0.7)

            if title is not None:
                ax.set_title(title)

            if windows_change:
                index += 1
                ax = axs[index]
                ax.plot(
                    df[AvellanedaStoikovParameters.midprice_period_window],
                    color=color,
                    lw=lw,
                    alpha=alpha,
                )
                ax.plot(
                    df[AvellanedaStoikovParameters.midprice_period_window]
                    .rolling(window=window)
                    .mean(),
                    color=color_mean,
                    lw=lw - 0.1,
                    alpha=alpha,
                )

                ax.set_ylabel(AvellanedaStoikovParameters.midprice_period_window)
                ax.grid(axis='y', ls='--', alpha=0.7)

            if skew_change:
                index += 1
                ax = axs[index]
                ax.plot(
                    df[AvellanedaStoikovParameters.skew],
                    color=color,
                    lw=lw,
                    alpha=alpha,
                )
                ax.set_ylabel(AvellanedaStoikovParameters.skew)
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
            import traceback

            traceback.print_exc()

        return None


if __name__ == '__main__':
    import os

    os.environ["LOG_STATE_STEPS"] = "1"  # print each step state in logs
    Algorithm.MULTITHREAD_CONFIGURATION = MultiThreadConfiguration.singlethread
    Algorithm.FEES_COMMISSIONS_INCLUDED = False
    Algorithm.DELAY_MS = 0
    instrument_pk = 'btcusdt_kraken'

    skew_action = [0.0, 1.0, -1.0]
    risk_aversion_action = [0.5, 0.25, 0.6, 0.9]
    midprice_period_window_action = [60, 120]

    QUANTITY = 0.001
    FIRST_HOUR = 8
    LAST_HOUR = 21

    most_significant_state_columns = []
    parameters_default_dqn = {
        # Q
        AlphaAvellanedaAlgorithmParameters.skew_action: skew_action,
        AlphaAvellanedaAlgorithmParameters.risk_aversion_action: risk_aversion_action,
        AlphaAvellanedaAlgorithmParameters.midprice_period_window_action: midprice_period_window_action,
        AvellanedaStoikovParameters.midprice_period_seconds: 15,
        AlphaAvellanedaAlgorithmParameters.change_k_period_seconds_action: [60],
        RlAlgorithmParameters.horizon_ticks_private_state: (5),
        RlAlgorithmParameters.horizon_ticks_market_state: (10),
        RlAlgorithmParameters.horizon_candles_state: (10),
        RlAlgorithmParameters.horizon_min_ms_tick: (5),
        RlAlgorithmParameters.score: ScoreEnum.asymmetric_dampened_pnl,  # ScoreEnum.asymmetric_dampened_pnl,
        RlAlgorithmParameters.step_seconds: (5),
        RlAlgorithmParameters.stop_action_on_filled: 0,
        # Avellaneda default
        AlgorithmParameters.quantity: (QUANTITY),
        AlgorithmParameters.first_hour: (FIRST_HOUR),
        AlgorithmParameters.last_hour: (LAST_HOUR),
        RlAlgorithmParameters.seed: 28220,
        RlAlgorithmParameters.rl_port: 2111,
        AlgorithmParameters.ui: 0,
        RlAlgorithmParameters.training_stats: False,
        RlAlgorithmParameters.action_type: ReinforcementLearningActionType.discrete,
        RlAlgorithmParameters.model: BaseModelType.PPO,
        RlAlgorithmParameters.custom_neural_networks: {"net_arch": [256, 256]},
    }
    # best_avellaneda_param_dict = {
    #     AvellanedaStoikovParameters.risk_aversion: 0.079665431,
    #     AvellanedaStoikovParameters.midprice_period_window: 60,
    #     AvellanedaStoikovParameters.seconds_change_k: 60
    # }

    algorithm_info_dqn = 'ppo_pytest'

    avellaneda_dqn = AlphaAvellanedaStoikov(
        algorithm_info=algorithm_info_dqn, parameters=parameters_default_dqn
    )
    # avellaneda_dqn.set_parameters(
    #     best_avellaneda_param_dict
    # )  # same optimization as benchmark

    # print('Starting training')
    output_train = avellaneda_dqn.train(
        instrument_pk=instrument_pk,
        start_date=datetime.datetime(year=2023, day=13, month=11, hour=7),
        end_date=datetime.datetime(year=2023, day=13, month=11, hour=15),
        iterations=10,
        simultaneous_algos=1,
        clean_initial_experience=True,
        plot_training=True,
        score_early_stopping=InfoStepKey.totalPnl,
        patience=5,
        min_iterations=35,
    )

    print('Starting testing')

    results = []
    scores = []
    # avellaneda_dqn.clean_model(output_path=BACKTEST_OUTPUT_PATH)
    iterations = 0
    explore_prob = 1.0

    parameters = avellaneda_dqn.get_parameters(explore_prob=explore_prob)
    avellaneda_dqn.set_parameters(parameters)

    output_test = avellaneda_dqn.test(
        instrument_pk=instrument_pk,
        start_date=datetime.datetime(year=2023, day=13, month=11, hour=10),
        end_date=datetime.datetime(year=2023, day=13, month=11, hour=12),
    )

    name_output = avellaneda_dqn.get_test_name(name=avellaneda_dqn.NAME)
    print(rf"output_test.keys() = {output_test.keys()}")
    backtest_df = output_test[name_output]

    score = get_score(
        backtest_df=backtest_df,
        score_enum=ScoreEnum.realized_pnl,
        equity_column_score=ScoreEnum.realized_pnl,
    )
    import matplotlib.pyplot as plt

    plt.figure()
    fig, df = avellaneda_dqn.plot_trade_results(
        raw_trade_pnl_df=output_test[name_output], title='test %d' % iterations
    )
    # fig.savefig(rf"{name_output}_test.png")
    # plt.show()
    #
    # avellaneda_dqn.plot_params(raw_trade_pnl_df=output_test[name_output])
    # plt.show()
