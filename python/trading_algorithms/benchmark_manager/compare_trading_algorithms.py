import datetime

import matplotlib.pyplot as plt
import pandas as pd

from trading_algorithms.algorithm import Algorithm
from trading_algorithms.market_making.avellaneda_stoikov import (
    AvellanedaStoikovParameters,
    AvellanedaStoikov,
)
from backtest.pnl_utils import *

from utils.pandas_utils.dataframe_utils import join_two_timeseries_different_index_ffill
import seaborn as sns
from scipy import stats
import datetime
import numpy as np
import pandas as pd
import tqdm

from backtest.parameter_tuning.ga_configuration import GAConfiguration


class ParameterTuningConfig:
    def __init__(
            self,
            ga_configuration: GAConfiguration,
            ga_configuration_max_simultaneous: int,
            algorithms_list: list,
            instrument_pk: str,
            start_date_train: datetime.datetime,
            end_date_train: datetime.datetime,
            parameters_min: list,
            parameters_max: list,
    ):
        self.ga_configuration_max_simultaneous = ga_configuration_max_simultaneous
        self.ga_configuration = ga_configuration
        self.algorithms_list = algorithms_list
        self.instrument_pk = instrument_pk
        self.start_date_train = start_date_train
        self.end_date_train = end_date_train

        self.parameters_min = parameters_min
        self.parameters_max = parameters_max


class CompareTradingAlgorithmsLauncher:
    def __init__(
            self,
            algorithms_list: list,
            instrument_pk: str,
            start_date_test: datetime.datetime,
            end_date_test: datetime.datetime,
            parameter_tuning_config: ParameterTuningConfig = None,
    ):

        self.algorithms_list = algorithms_list
        self.instrument_pk = instrument_pk
        self.start_date_test = start_date_test
        self.end_date_test = end_date_test
        timespan_test = self.end_date_test - self.start_date_test
        if timespan_test.days > 0:
            print(
                rf"WARNING : TestLauncher is going to launch a test with a timespan of {timespan_test.days} days"
            )

        self.parameter_tuning_config = parameter_tuning_config

    def _launch_parameter_tuning(self):
        parameter_tuning_algorithms = self.parameter_tuning_config.algorithms_list
        for index, algorithm in enumerate(parameter_tuning_algorithms):
            print(rf"{algorithm.algorithm_info} launch_parameter_tuning ...")
            best_param_dict, summary_df = algorithm.parameter_tuning(
                instrument_pk=self.parameter_tuning_config.instrument_pk,
                start_date=self.parameter_tuning_config.start_date_train,
                end_date=self.parameter_tuning_config.end_date_train,
                parameters_min=self.parameter_tuning_config.parameters_min[index],
                parameters_max=self.parameter_tuning_config.parameters_max[index],
                ga_configuration=self.parameter_tuning_config.ga_configuration,
                max_simultaneous=self.parameter_tuning_config.ga_configuration_max_simultaneous,
            )
            print(rf"{algorithm.algorithm_info} best_param_dict-> {best_param_dict}")
            algorithm.set_parameters(best_param_dict)

    def _launch_test_algorithm(self, algorithm, output_dict: dict):
        algo_info_to_df_dict = algorithm.test(
            start_date=self.start_date_test,
            end_date=self.end_date_test,
            instrument_pk=self.instrument_pk,
        )
        # output_dict[algorithm] = algo_info_to_df_dict
        output_dict[algorithm] = algo_info_to_df_dict[algorithm.algorithm_info]

    def _launch_train_algorithm(self, algorithm, episodes, simultaneous_algos):
        from trading_algorithms.reinforcement_learning.rl_algorithm import RLAlgorithm
        if isinstance(algorithm, RLAlgorithm):
            all_infos = algorithm.train(
                start_date=self.start_date_test,
                end_date=self.end_date_test,
                instrument_pk=self.instrument_pk,
                iterations=episodes,
                simultaneous_algos=simultaneous_algos,
                clean_initial_experience=False,
                use_validation_callback=False,
                plot_training=False,
            )




    def launch_test(self, max_simultaneous: int = -2) -> dict:
        '''

        Returns dict with key=algorithm and value=DataFrame with columns:
        -------

        '''
        output_test = {}
        if self.parameter_tuning_config is not None:
            self._launch_parameter_tuning()

        if max_simultaneous == 1:
            for algorithm in tqdm.tqdm(self.algorithms_list):
                self._launch_test_algorithm(algorithm, output_test)
        else:
            from utils.paralellization_util import process_jobs_joblib

            jobs = []
            for algorithm in self.algorithms_list:
                jobs.append(
                    {
                        "func": self._launch_test_algorithm,
                        "algorithm": algorithm,
                        "output_dict": output_test,
                    }
                )
            process_jobs_joblib(jobs, num_threads=max_simultaneous)

        return output_test

    def launch_train(self, episodes, simultaneous_algos, max_simultaneous: int = -2):
        '''

        Returns nothing
        -------

        '''

        rl_algorithms = []
        for algorithm in self.algorithms_list:
            from trading_algorithms.reinforcement_learning.rl_algorithm import RLAlgorithm
            if isinstance(algorithm, RLAlgorithm):
                rl_algorithms.append(algorithm)
        print(
            rf"training {len(rl_algorithms)} rl_algorithms {episodes} episodes {simultaneous_algos} simultaneous_algos in parallel {max_simultaneous} ")

        if max_simultaneous == 1:
            for algorithm in tqdm.tqdm(rl_algorithms):
                self._launch_train_algorithm(algorithm, episodes, simultaneous_algos)
        else:
            from utils.paralellization_util import process_jobs_joblib

            jobs = []
            for algorithm in rl_algorithms:
                jobs.append(
                    {
                        "func": self._launch_train_algorithm,
                        "algorithm": algorithm,
                        "episodes": episodes,
                        "simultaneous_algos": simultaneous_algos
                    }
                )
            process_jobs_joblib(jobs, num_threads=max_simultaneous)


class CompareTradingAlgorithms:
    '''
    CompareTradingAlgorithms class to add results of backtest of all the algorithms and all the days

    '''

    COLOR_PALETTE = "terrain_r"
    SCORE_ENUMS = [
        ScoreEnum.total_pnl,
        ScoreEnum.realized_pnl,
        ScoreEnum.unrealized_pnl,
        ScoreEnum.number_trades,
        ScoreEnum.sharpe,
        ScoreEnum.sortino,
        ScoreEnum.max_dd,
        ScoreEnum.pnl_to_map,
    ]
    DEFAULT_RATIO_VALUE = 0.0

    def __init__(self, strategies_benchmark: dict, strategies_test_output: dict):
        '''

        Parameters
        ----------
        strategies_benchmark : dictionary with key=algorithm and value=bool true or false
        strategies_test_output : dictionary with key=algorithm and value is a dictionary key algorithm_info and value DataFrame
        '''
        self.strategies_benchmark = strategies_benchmark  # dict : algorithm,bool
        self.strategies_test_output = (
            strategies_test_output  # dict : algorithm, DataFrame
        )

        self._set_counts()
        self._set_colors()

    @staticmethod
    def get_instance(
            algorithms_list: list,
            benchmark_algorithms: list,
            instrument_pk: str,
            start_date: datetime.datetime,
            end_date: datetime.datetime,
            max_simultaneous: int = -2,
            train_episodes: int = 0,
            train_simultaneous_algos: int = 0,
    ):
        compare_trading_launcher = CompareTradingAlgorithmsLauncher(
            algorithms_list=algorithms_list,
            instrument_pk=instrument_pk,
            start_date_test=start_date,
            end_date_test=end_date,
        )
        result_compare = compare_trading_launcher.launch_test(max_simultaneous)
        if (train_episodes > 0):
            compare_trading_launcher.launch_train(train_episodes, train_simultaneous_algos, max_simultaneous)

        # benchmark_algorithms_dict = {algorithm: False for algorithm not in benchmark_algorithms}
        benchmark_algorithms_dict = {}
        for algorithm in algorithms_list:
            benchmark_algorithms_dict[algorithm] = False
            if algorithm in benchmark_algorithms:
                benchmark_algorithms_dict[algorithm] = True

        compare_trading_algorithm = CompareTradingAlgorithms(
            strategies_benchmark=benchmark_algorithms_dict,
            strategies_test_output=result_compare,
        )
        return compare_trading_algorithm

    def _set_counts(self):
        self.benchmark_algorithm_count = 0
        for algorithm, is_benchmark in self.strategies_benchmark.items():
            if is_benchmark:
                self.benchmark_algorithm_count += 1
        self.non_benchmark_algorithm_count = (
                len(self.strategies_benchmark) - self.benchmark_algorithm_count
        )

    def _set_colors(self):
        # palette = sns.color_palette("terrain_r", self.non_benchmark_algorithm_count)
        # palette_benchmark = sns.color_palette("Greys",self.benchmark_algorithm_count)
        palette_all = sns.color_palette(
            CompareTradingAlgorithms.COLOR_PALETTE, len(self.strategies_benchmark)
        )

        index_benchmark = 0
        index_algorithm = 0
        index_all = 0
        self.strategies_color = {}
        for algorithm, is_benchmark in self.strategies_benchmark.items():
            # if is_benchmark:
            #     self.strategies_color[algorithm]=palette_benchmark[index_benchmark]
            #     index_benchmark+=1
            # else:
            #     self.strategies_color[algorithm]=palette[index_algorithm]
            #     index_algorithm+=1
            self.strategies_color[algorithm] = palette_all[index_all]
            index_all += 1

    @staticmethod
    def get_output_test_df(output_test: dict, index: int = 0) -> pd.DataFrame:
        key = list(output_test.keys())[index]
        df = output_test[key]
        return df

    @staticmethod
    def _get_empty_backtest_metric():
        results_dict = {}
        results_dict['date'] = None
        for score_enum in CompareTradingAlgorithms.SCORE_ENUMS:
            results_dict[score_enum] = CompareTradingAlgorithms.DEFAULT_RATIO_VALUE
        return results_dict

    @staticmethod
    def get_final_metrics(
            backtest_df: pd.DataFrame,
            equity_column_score_enum: ScoreEnum = ScoreEnum.realized_pnl,
    ) -> dict:
        # df = CompareTradingAlgorithms.get_output_test_df(output_test, 0)
        results_dict = {}
        if backtest_df is not None and len(backtest_df) > 0:
            for score_enum in CompareTradingAlgorithms.SCORE_ENUMS:
                ratio_value = get_score(
                    backtest_df=backtest_df,
                    score_enum=score_enum,
                    equity_column_score=equity_column_score_enum,
                )
                results_dict[score_enum] = ratio_value
        else:
            print(
                rf"WARNING : backtest_df is None or len(backtest_df)==0 -> set all values to {CompareTradingAlgorithms.DEFAULT_RATIO_VALUE}"
            )
            return CompareTradingAlgorithms._get_empty_backtest_metric()

        initial_date = backtest_df['date'].iloc[0]
        if isinstance(initial_date, str):
            backtest_df['date'] = pd.to_datetime(backtest_df['date'])

        start_day = backtest_df['date'].iloc[0].date()
        last_day = backtest_df['date'].iloc[-1].date()
        if start_day == last_day:
            results_dict['date'] = start_day
        else:
            # TODO : review when more than one day
            results_dict['date'] = rf"{start_day}_{last_day}"
        return results_dict

    @staticmethod
    def _get_column_name(algorithm: Algorithm):
        # column_name=list(test_result.keys())[0]
        column_name = type(algorithm).__name__
        is_avellaneda_stoikov = isinstance(algorithm, AvellanedaStoikov)
        if is_avellaneda_stoikov:
            algorithm_as = algorithm
            risk_aversion = algorithm_as.parameters[
                AvellanedaStoikovParameters.risk_aversion
            ]
            windows_tick = algorithm_as.parameters[
                AvellanedaStoikovParameters.midprice_period_window
            ]
            column_name = f'{column_name}_{risk_aversion}_{windows_tick}'
        return column_name

    def get_results(self, equity_column_score_enum=ScoreEnum.unrealized_pnl) -> dict:
        output_dict = {}
        are_empty_algos = False
        for algorithm, backtest_df in self.strategies_test_output.items():
            if backtest_df is None:
                print(
                    rf"error on get_results algorithm strategies_test_output output  {algorithm.algorithm_info} is None-> add something empty"
                )
                output_dict[
                    algorithm
                ] = CompareTradingAlgorithms._get_empty_backtest_metric()
                are_empty_algos = True
                continue

            column_name = self._get_column_name(algorithm)

            final_metrics = CompareTradingAlgorithms.get_final_metrics(
                backtest_df, equity_column_score_enum=equity_column_score_enum
            )
            final_metrics['name'] = column_name
            output_dict[algorithm] = final_metrics
        if are_empty_algos:
            output_df = pd.DataFrame.from_dict(output_dict)
            output_df = output_df.transpose()
            output_df['date'].ffill(inplace=True)
            output_df['date'].bfill(inplace=True)
            output_df = output_df.transpose()
            output_dict = output_df.to_dict()

        return output_dict

    def get_algorithm(self, algorithm_info: str) -> Algorithm:
        for algorithm, is_benchmark in self.strategies_benchmark.items():
            if algorithm.algorithm_info == algorithm_info:
                return algorithm

    def get_algorithm_info_color(self):
        output = {}
        for algorithm, color in self.strategies_color.items():
            output[algorithm.algorithm_info] = color
        return output

    def is_benchmark_algorithm(self, algorithm_info: str) -> bool:
        algo = self.get_algorithm(algorithm_info=algorithm_info)
        return self.strategies_benchmark.get(algo, False)

    def plot_equity_curve(
            self,
            figsize=None,
            title: str = None,
            plot_equity_column_score_enum=ScoreEnum.realized_pnl,
            metrics_equity_column_score_enum=ScoreEnum.realized_pnl,
    ) -> (pd.DataFrame, plt.figure):
        '''

        Parameters
        ----------
        figsize
        title
        plot_equity_column_score_enum
        metrics_equity_column_score_enum

        Returns fig,output_plot_df
        -------

        '''
        import seaborn as sns

        sns.set_theme()
        import matplotlib.pyplot as plt

        if figsize is None:
            figsize = (20, 12)

        output_plot_df = None
        legends = []
        plt.close()

        if title is None:
            title = rf'Equity curve:{plot_equity_column_score_enum} (€)'

        benchmark_algorithm_count = 0

        for algorithm, backtest_df in self.strategies_test_output.items():
            if backtest_df is None:
                print(
                    rf"ERROR: on plot_equity_curve algorithm strategies_test_output output  {algorithm.algorithm_info} is None-> skip it"
                )
                continue

            final_metrics = CompareTradingAlgorithms.get_final_metrics(
                backtest_df, equity_column_score_enum=metrics_equity_column_score_enum
            )
            legend_str = rf'{algorithm.algorithm_info}     sharpe={final_metrics[ScoreEnum.sharpe]:.2f} realized_pnl={final_metrics[ScoreEnum.realized_pnl]:.2f} total_pnl={final_metrics[ScoreEnum.total_pnl]:.2f}'
            if self.strategies_benchmark[algorithm]:
                benchmark_algorithm_count += 1

            if backtest_df is not None and len(backtest_df) > 0:
                backtest_df.set_index('date', inplace=True)
                backtest_df.sort_index(inplace=True)

                series_to_plot = backtest_df[
                    get_score_enum_csv_column(plot_equity_column_score_enum)
                ]
                series_to_plot.index = pd.to_datetime(series_to_plot.index)

                dataframe_to_combine = series_to_plot.to_frame(
                    name=algorithm.algorithm_info
                )

                if output_plot_df is None:
                    output_plot_df = dataframe_to_combine
                else:
                    output_plot_df = join_two_timeseries_different_index_ffill(
                        output_plot_df, dataframe_to_combine
                    )
                legends.append(legend_str)

        fig, axs = plt.subplots(nrows=1, ncols=1, figsize=figsize)
        ax = axs

        alpha = 1.0
        lw = 0.8

        alpha_benchmark = 0.8
        lw_benchmark = 0.5

        index_algorithm = 0
        index_benchmark = 0
        for algorithm_info in list(output_plot_df.columns):
            algorithm = self.get_algorithm(algorithm_info=algorithm_info)
            alpha_set = alpha
            lw_set = lw
            index_set = index_algorithm

            is_benchmark = self.is_benchmark_algorithm(algorithm_info)
            if is_benchmark:
                alpha_set = alpha_benchmark
                lw_set = lw_benchmark
                index_set = index_benchmark

            series_to_plot_final = output_plot_df[algorithm_info]
            color = self.strategies_color[algorithm]
            ax.plot(series_to_plot_final, color=color, lw=lw_set, alpha=alpha_set)

            if is_benchmark:
                index_benchmark += 1
            else:
                index_algorithm += 1

        ax.legend(legends, loc='best')
        ax.grid(axis='y', ls='--', alpha=0.7)
        ax.set_xlabel('time')
        ax.set_ylabel('€')
        ax.set_title(title)
        plt.show()

        return (output_plot_df, fig)


###
def main():
    start_date = datetime.datetime(2022, 1, 15, 11)
    end_date = datetime.datetime(2022, 1, 15, 12)
    instrument_pk = 'btcusdt_binance'
    from trading_algorithms.market_making.avellaneda_stoikov import AvellanedaStoikov
    from trading_algorithms.market_making.avellaneda_stoikov import (
        AvellanedaStoikovParameters,
    )

    avellaneda_stoikov = AvellanedaStoikov(
        algorithm_info='AvellanedaStoikovTest1',
        parameters={
            AvellanedaStoikovParameters.risk_aversion: 0.9,
            AvellanedaStoikovParameters.midprice_period_seconds: 1,
            AvellanedaStoikovParameters.midprice_period_window: 15,
            AvellanedaStoikovParameters.seconds_change_k: 5,
        },
    )
    from trading_algorithms.market_making.constant_spread import ConstantSpread
    from trading_algorithms.market_making.constant_spread import (
        ConstantSpreadParameters,
    )

    constant_spread2 = ConstantSpread(
        algorithm_info='ConstantSpreadTest1',
        parameters={ConstantSpreadParameters.level: 0},
    )
    constant_spread = ConstantSpread(
        algorithm_info='ConstantSpreadTest2',
        parameters={ConstantSpreadParameters.level: 1},
    )

    algorithms_list = [
        avellaneda_stoikov,
        # constant_spread,
        constant_spread2,
    ]
    compare_trading = CompareTradingAlgorithms.get_instance(
        algorithms_list=algorithms_list,
        instrument_pk=instrument_pk,
        start_date=start_date,
        end_date=end_date,
        benchmark_algorithms=[avellaneda_stoikov],
    )
    compare_trading.plot_equity_curve()


if __name__ == '__main__':
    main()
