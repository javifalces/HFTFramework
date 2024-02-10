from datetime import datetime, timedelta

import pandas as pd
from scipy.stats import mannwhitneyu, kruskal

from configuration import LAMBDA_OUTPUT_PATH
from trading_algorithms.score_enum import ScoreEnum
import numpy as np
import matplotlib.pyplot as plt
import os


class CompareStatisticallyTradingAlgorithms:
    persist_path = rf"{LAMBDA_OUTPUT_PATH}/compare_statistically_trading_algorithms"
    if not os.path.exists(persist_path):
        os.mkdir(path=persist_path)

    ratios = [
        ScoreEnum.sharpe,
        ScoreEnum.sortino,
        ScoreEnum.max_dd,
        ScoreEnum.pnl_to_map,
    ]

    @staticmethod
    def load(name: str):
        import pickle

        path_to_load = (
            rf"{CompareStatisticallyTradingAlgorithms.persist_path}/{name}.pkl"
        )
        with open(path_to_load, 'rb') as handle:
            return pickle.load(handle)

    @staticmethod
    def clean(name: str):
        import pickle

        path_to_load = (
            rf"{CompareStatisticallyTradingAlgorithms.persist_path}/{name}.pkl"
        )
        os.remove(path_to_load)

    def __init__(self):
        self.results_compare_dict = {}  # algorithm_info and results
        pass

    def add_results_compare(self, results_output_dict: dict):
        '''

        Parameters
        ----------
        results_output_dict: dict of algorithm and test results df

        Returns
        -------

        '''
        for algorithm, final_metrics in results_output_dict.items():
            # update dict by algorithm
            if algorithm.algorithm_info not in self.results_compare_dict.keys():
                self.results_compare_dict[algorithm.algorithm_info] = []

            self.results_compare_dict[algorithm.algorithm_info].append(final_metrics)

    def save(self, name: str):
        import pickle

        path_to_save = (
            rf"{CompareStatisticallyTradingAlgorithms.persist_path}/{name}.pkl"
        )
        with open(path_to_save, 'wb') as handle:
            pickle.dump(self, handle, protocol=pickle.HIGHEST_PROTOCOL)

    def get_results_compare(self, score_enum: ScoreEnum) -> pd.DataFrame:
        all_columns = {}
        output_df = None
        dates = []
        for algorithm_info, final_metrics_list in self.results_compare_dict.items():
            scores = []
            for final_metric_day in final_metrics_list:
                scores.append(final_metric_day[score_enum])
                dates.append(final_metric_day['date'])
            all_columns[algorithm_info] = scores
        if len(all_columns) > 0:
            dates = list(set(dates))
            dates.sort()
            output_df = pd.DataFrame.from_dict(all_columns)
            output_df['date'] = dates
            output_df.set_index('date', inplace=True)
        return output_df

    @staticmethod
    def get_algo_name(column):
        splitted_column = column.split('_')
        algo_name = rf"{splitted_column[0]}_{splitted_column[1]}"
        if splitted_column[2] == 'rnn':
            #     if column.startswith('alpha_avellaneda'):
            algo_name = (
                rf"{splitted_column[0]}_{splitted_column[1]}_{splitted_column[2]}"
            )
        return algo_name

    def plot_results_compare(
            self,
            score_enum: ScoreEnum,
            results_compare_df=None,
            figsize=None,
            algorithm_info_color=None,
    ) -> (pd.DataFrame, plt.figure):
        import seaborn as sns

        sns.set_theme()
        import matplotlib.pyplot as plt

        if figsize is None:
            figsize = (20, 12)

        if results_compare_df is None:
            results_compare_df = self.get_results_compare(score_enum=score_enum)

        if algorithm_info_color is None:
            from trading_algorithms.benchmark_manager.compare_trading_algorithms import (
                CompareTradingAlgorithms,
            )

            palette_all = sns.color_palette(
                CompareTradingAlgorithms.COLOR_PALETTE, results_compare_df.shape[1]
            )
            sns.set_palette(palette_all)

        # DD is reversed!! so highest is the best!
        # if score_enum is ScoreEnum.max_dd:
        #     list_rows_wins=pd.Series(results_compare_df.astype(float).idxmin(axis=1).values.tolist()).value_counts()
        # else:

        list_rows_wins = pd.Series(
            results_compare_df.astype(float).idxmax(axis=1).values.tolist()
        ).value_counts()
        best_algorithm = list_rows_wins.keys()[list_rows_wins.argmax()]

        plt.close()
        fig, axs = plt.subplots(nrows=2, ncols=1, figsize=figsize)
        ax = axs[0]

        plot_all = True
        if algorithm_info_color is not None:
            plot_all = False
            try:
                for column in list(results_compare_df.columns):
                    series = results_compare_df[column]
                    series.plot.bar(ax=ax, color=algorithm_info_color[column])
            except Exception as e:
                print(rf"WARNING: error plotting bar by days {e}")
                plot_all = True

        if plot_all:
            results_compare_df.plot.bar(ax=ax)

        ax.set_ylabel(rf'{score_enum}')
        ax.set_title(
            rf"[{score_enum}] best algorithm:{best_algorithm}  winning days:{list_rows_wins[best_algorithm]}"
        )
        ax.legend(loc='best')
        ax = axs[1]
        plot_all = True

        # if algorithm_info_color is not None:
        #     plot_all=False
        #     try:
        #         for name in list(list_rows_wins.keys()):
        #             value=list_rows_wins[name]
        #             value.plot.barh(ax=ax,color=algorithm_info_color[name])
        #     except Exception as e:
        #         print(rf"WARNING: error plotting horizontal bar winning days {e}")
        #         plot_all=True
        #
        # if plot_all:
        list_rows_wins.plot.barh(ax=ax)

        # ax.set_title('winning days')
        ax.set_xlabel('winning days')
        # ax.set_ylabel('algorithm')
        plt.tight_layout()
        plt.show()
        return (results_compare_df, fig)

    def get_mann_whitney(
            self, score_enum: ScoreEnum, benchmark_algo_info: str = 'avellaneda_stoikov'
    ) -> pd.Series:
        '''

        Parameters
        ----------
        score_enum
        benchmark_algo_info

        Returns
        -------

        '''
        from statsmodels.stats import weightstats as stests

        columns_df = self.get_results_compare(score_enum=score_enum)
        # all ratios vs avellaneda_stoikov column
        output_dict = {}
        for column in columns_df.columns:
            if column == benchmark_algo_info:
                continue
            #         stats_values=stats.mannwhitneyu(columns_df[column].values,columns_df[benchmark_algo].values,alternative="greater")
            # {‘two-sided’, ‘less’, ‘greater’}
            stats_values = mannwhitneyu(
                columns_df[column].fillna(0).values,
                columns_df[benchmark_algo_info].fillna(0).values,
                alternative="two-sided",
            )
            p_value_ponferroni = stats_values.pvalue / columns_df.shape[0]

            #         corr, _ = stats.pearsonr(columns_df[column].values,columns_df[benchmark_algo].values)
            #         z= z_score(columns_df[column].values,columns_df[benchmark_algo].values)
            z, propability_value = stests.ztest(
                columns_df[column].values, columns_df[benchmark_algo_info].values
            )
            r = -z / np.sqrt(columns_df.shape[0])

            result_str = rf"U = {stats_values.statistic:0.1f}  p_value={p_value_ponferroni:0.2e} r={r:0.2f} "
            output_dict[column] = result_str
        return pd.Series(output_dict)

    def get_kruskal_wallis(self, score_enum: ScoreEnum) -> (float, float, float):
        results_df = self.get_results_compare(score_enum)
        result = self._get_kruskal_wallis(results_df)
        p_value_ponferroni = result.pvalue / results_df.shape[0]
        print(
            rf"H({len(self.results_compare_dict)})_{score_enum} = {result.statistic:0.2f}  p_value={p_value_ponferroni:0.2e}"
        )
        return (result.statistic, result.pvalue, p_value_ponferroni)

    def _get_kruskal_wallis(self, results_df):
        '''

         Parameters
         ----------
         results_df

         Returns

         -------
         statistic : float
        The Kruskal-Wallis H statistic, corrected for ties.
         pvalue : float
        The p-value for the test using the assumption that H has a chi
        square distribution. The p-value returned is the survival function of
        the chi square distribution evaluated at H.

        '''
        num_columns = results_df.shape[1]
        column_values = [results_df.fillna(0).values[:, i] for i in range(num_columns)]
        return kruskal(*column_values)

    @staticmethod
    def get_column_score(input_df, column_score):
        output_df = pd.DataFrame(index=input_df.index)
        for column in input_df.columns:
            is_my_column = column.endswith(column_score) and '_' in column
            is_better_column = column.startswith('is_better')
            if is_my_column and not is_better_column:
                try:
                    #             import pdb; pdb.set_trace()
                    #                 splitted_column=column.split('_')
                    algo_name = CompareStatisticallyTradingAlgorithms.get_algo_name(
                        column
                    )
                    output_df[algo_name] = input_df[column]
                except Exception as e:
                    print(rf"error on column {column}  {e}")
        if '2020-12-21' in output_df.index:
            output_df.drop('2020-12-21', inplace=True)
        return output_df

    @staticmethod
    def print_mann_whitney_table(input_df):
        output_df = pd.DataFrame()
        for ratio in CompareStatisticallyTradingAlgorithms.ratios:
            series_ratio = CompareStatisticallyTradingAlgorithms.get_mann_whitney(
                input_df, ratio
            )
            output_df[ratio] = series_ratio
        return output_df

    @staticmethod
    def print_kruskal_wallis(input_df):
        output_str = ''
        for ratio in CompareStatisticallyTradingAlgorithms.ratios:
            result = CompareStatisticallyTradingAlgorithms.get_kruskal_wallis(
                input_df, ratio
            )
            if result is None:
                print(
                    rf"print_kruskal_wallis get_kruskal_wallis is None on ratio : {ratio}"
                )
                continue

            p_value_ponferroni = result.pvalue / input_df.shape[0]
            output_str += (
                    rf"H(4)_{ratio} = {result.statistic:0.2f}  p_value={p_value_ponferroni:0.2e}"
                    + "\n"
            )
        return output_str

    # function to calculate Cohen's d for independent samples
    @staticmethod
    def cohend(d1, d2):
        import numpy as np
        from numpy import mean, var, sqrt

        # calculate the size of samples
        n1, n2 = len(d1), len(d2)
        # calculate the variance of the samples
        s1, s2 = var(d1, ddof=1), var(d2, ddof=1)
        # calculate the pooled standard deviation
        s = sqrt(((n1 - 1) * s1 + (n2 - 1) * s2) / (n1 + n2 - 2))
        # calculate the means of the samples
        u1, u2 = mean(d1), mean(d2)
        # calculate the effect size
        return (u1 - u2) / s

    @staticmethod
    def z_score(d1, d2):
        import numpy as np
        from numpy import mean, var, sqrt

        # calculate the size of samples
        n1, n2 = len(d1), len(d2)
        distance = d1 - d2

        mean_distance = mean(distance)
        std_distance = sqrt(var(distance))
        return mean((distance - mean_distance) / std_distance)


###
def main():
    start_date = datetime(2022, 1, 15, 11)
    end_date = datetime(2022, 1, 15, 12)
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
    from trading_algorithms.market_making.alpha_avellaneda_stoikov import AlphaAvellanedaStoikov
    alpha_avellaneda_stoikov = AlphaAvellanedaStoikov(
        algorithm_info='AlphaAvellanedaStoikovTest1',

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
        alpha_avellaneda_stoikov,
        # avellaneda_stoikov,
        # constant_spread,
        # constant_spread2,
    ]
    from trading_algorithms.benchmark_manager.compare_trading_algorithms import (
        CompareTradingAlgorithms,
    )

    compare_trading1 = CompareTradingAlgorithms.get_instance(
        algorithms_list=algorithms_list,
        instrument_pk=instrument_pk,
        start_date=start_date,
        end_date=end_date,
        benchmark_algorithms=[avellaneda_stoikov],
        max_simultaneous=1,

        train_episodes=2,
        train_simultaneous_algos=1
    )
    compare_trading1_results = compare_trading1.get_results(
        equity_column_score_enum=ScoreEnum.total_pnl
    )
    # compare_trading1.plot_equity_curve(plot_equity_column_score_enum=ScoreEnum.total_pnl)

    compare_trading2 = CompareTradingAlgorithms.get_instance(
        algorithms_list=algorithms_list,
        instrument_pk=instrument_pk,
        start_date=start_date + timedelta(days=1),
        end_date=end_date + timedelta(days=1),
        benchmark_algorithms=[avellaneda_stoikov],
        max_simultaneous=1,
    )
    compare_trading2_results = compare_trading2.get_results(
        equity_column_score_enum=ScoreEnum.total_pnl
    )

    compare_trading_statistically = CompareStatisticallyTradingAlgorithms()
    compare_trading_statistically.add_results_compare(compare_trading1_results)
    compare_trading_statistically.add_results_compare(compare_trading2_results)

    sharpe_df = compare_trading_statistically.get_results_compare(
        score_enum=ScoreEnum.sharpe
    )
    compare_trading_statistically.print_kruskal_wallis(sharpe_df)

    print(sharpe_df.head())


if __name__ == '__main__':
    main()
