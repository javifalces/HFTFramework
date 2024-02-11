import unittest

import matplotlib.pyplot as plt
import copy
from backtest.pnl_utils import *
from trading_algorithms.benchmark_manager.compare_trading_algorithms import (
    CompareTradingAlgorithms,
)
from trading_algorithms.trading_algorithms_import import *
import pathlib


class CompareTradingAlgorithmsTest(unittest.TestCase):
    pwd = pathlib.Path(__file__).parent.resolve()
    sample_output_test_df = pd.read_csv(rf'{pwd}/../../data/trades_table_sample.csv')
    instrument_pk = sample_output_test_df['instrument'].iloc[0]

    def generate_backtest_df(self):
        output_df = copy.copy(self.sample_output_test_df)

        index_new = pd.to_datetime(output_df['date']) + pd.to_timedelta(
            np.random.randint(-800, 800, len(output_df)), unit='ms'
        )
        output_df['date'] = index_new
        output_df = output_df.set_index('date').sort_index().reset_index()

        returns_df = pd.Series(np.random.random(output_df.shape[0]))
        equity_curve_random = returns_df.cumsum()
        output_df[
            get_score_enum_csv_column(ScoreEnum.unrealized_pnl)
        ] = equity_curve_random

        returns_df1 = pd.Series(np.random.random(output_df.shape[0]))
        equity_curve_random1 = returns_df1.cumsum()

        output_df[
            get_score_enum_csv_column(ScoreEnum.realized_pnl)
        ] = equity_curve_random1

        output_df[get_score_enum_csv_column(ScoreEnum.total_pnl)] = (
                equity_curve_random1 + equity_curve_random
        )

        return output_df

    def generate_algorithm(
            self, algorithm_enum: AlgorithmEnum, algorithm_info: str
    ) -> Algorithm:
        return get_algorithm(algorithm_enum)(algorithm_info)

    def _generate_inputs_compare(self):
        strategies_test_output = {}
        strategies_benchmark = {}

        as_1 = self.generate_algorithm(
            algorithm_enum=AlgorithmEnum.avellaneda_stoikov,
            algorithm_info='as_benchmark1',
        )
        as_2 = self.generate_algorithm(
            algorithm_enum=AlgorithmEnum.avellaneda_stoikov,
            algorithm_info='as_benchmark2',
        )
        as_1.parameters[AvellanedaStoikovParameters.risk_aversion] = 1.01
        as_2.parameters[AvellanedaStoikovParameters.risk_aversion] = 0.01
        algorithms = [as_1, as_2]

        other_algorithms = [AlgorithmEnum.constant_spread, AlgorithmEnum.linear_constant_spread]
        for algorithm_enum in other_algorithms:
            algorithm = self.generate_algorithm(
                algorithm_enum=algorithm_enum,
                algorithm_info=str(algorithm_enum) + '_other',
            )
            algorithms.append(algorithm)

        for algorithm in algorithms:
            strategies_test_output[algorithm] = self.generate_backtest_df()
            strategies_benchmark[algorithm] = False
            if isinstance(algorithm, AvellanedaStoikov):
                strategies_benchmark[algorithm] = True
        return (strategies_benchmark, strategies_test_output)

    def test_get_results(self):
        (strategies_benchmark, strategies_test_output) = self._generate_inputs_compare()
        compare_trading_strategies = CompareTradingAlgorithms(
            strategies_benchmark=strategies_benchmark,
            strategies_test_output=strategies_test_output,
        )
        results_df = compare_trading_strategies.get_results(
            equity_column_score_enum=ScoreEnum.unrealized_pnl
        )
        self.assertIsNotNone(results_df)
        self.assertTrue(len(results_df) > 0)

    def test_get_results_empty(self):
        (strategies_benchmark, strategies_test_output) = self._generate_inputs_compare()
        strategy_to_none = list(strategies_test_output.keys())[0]

        strategies_test_output[strategy_to_none] = None
        compare_trading_strategies = CompareTradingAlgorithms(
            strategies_benchmark=strategies_benchmark,
            strategies_test_output=strategies_test_output,
        )
        results_df = compare_trading_strategies.get_results(
            equity_column_score_enum=ScoreEnum.unrealized_pnl
        )
        self.assertIsNotNone(results_df)
        self.assertTrue(len(results_df) > 0)

        strategies_test_output[strategy_to_none] = pd.DataFrame()
        compare_trading_strategies = CompareTradingAlgorithms(
            strategies_benchmark=strategies_benchmark,
            strategies_test_output=strategies_test_output,
        )
        results_df = compare_trading_strategies.get_results(
            equity_column_score_enum=ScoreEnum.unrealized_pnl
        )
        self.assertIsNotNone(results_df)
        self.assertTrue(len(results_df) > 0)

    @unittest.skip("todo:change date each date generated")
    def test_get_results_compare_days(self):
        days = 3
        compare_trading_statc = CompareStatisticallyTradingAlgorithms()
        for day in range(days):
            (
                strategies_benchmark,
                strategies_test_output,
            ) = self._generate_inputs_compare()
            compare_trading_strategies = CompareTradingAlgorithms(
                strategies_benchmark=strategies_benchmark,
                strategies_test_output=strategies_test_output,
            )
            results_output_dict = compare_trading_strategies.get_results(
                equity_column_score_enum=ScoreEnum.unrealized_pnl
            )
            compare_trading_statc.add_results_compare(results_output_dict)

        sharpe_df = compare_trading_statc.get_results_compare(
            score_enum=ScoreEnum.sharpe
        )
        self.assertIsNotNone(sharpe_df)
        self.assertTrue(len(sharpe_df) == days)

        sortino_df = compare_trading_statc.get_results_compare(
            score_enum=ScoreEnum.sortino
        )
        self.assertIsNotNone(sortino_df)
        self.assertTrue(len(sortino_df) == days)

        # test save and load
        self.assertNotAlmostEqual(
            sortino_df.sum().sum(), sharpe_df.sum().sum(), delta=0.01
        )
        compare_trading_statc.save("test_compare_trading_algorithms")

        compare_trading_statc2 = CompareStatisticallyTradingAlgorithms.load(
            "test_compare_trading_algorithms"
        )

        sortino_df2 = compare_trading_statc2.get_results_compare(
            score_enum=ScoreEnum.sortino
        )
        sharpe_df2 = compare_trading_statc2.get_results_compare(
            score_enum=ScoreEnum.sharpe
        )
        self.assertEqual(sortino_df.sum().sum(), sortino_df2.sum().sum())
        self.assertEqual(sharpe_df.sum().sum(), sharpe_df2.sum().sum())

        CompareStatisticallyTradingAlgorithms.clean("test_compare_trading_algorithms")

    @unittest.skip("todo:change date each date generated")
    def test_get_results_compare_days_some_days_wrong(self):
        days = 3
        compare_trading_statc = CompareStatisticallyTradingAlgorithms()

        for day in range(days):
            (
                strategies_benchmark,
                strategies_test_output,
            ) = self._generate_inputs_compare()
            if day == 0:
                strategy_to_none = list(strategies_test_output.keys())[0]
                strategies_test_output[strategy_to_none] = pd.DataFrame()

            compare_trading_strategies = CompareTradingAlgorithms(
                strategies_benchmark=strategies_benchmark,
                strategies_test_output=strategies_test_output,
            )
            results_output_dict = compare_trading_strategies.get_results(
                equity_column_score_enum=ScoreEnum.unrealized_pnl
            )
            compare_trading_statc.add_results_compare(results_output_dict)

        sharpe_df = compare_trading_statc.get_results_compare(
            score_enum=ScoreEnum.sharpe
        )
        self.assertIsNotNone(sharpe_df)
        self.assertTrue(len(sharpe_df) == days)

        sortino_df = compare_trading_statc.get_results_compare(
            score_enum=ScoreEnum.sortino
        )
        self.assertIsNotNone(sortino_df)
        self.assertTrue(len(sortino_df) == days)

        self.assertNotAlmostEqual(
            sortino_df.sum().sum(), sharpe_df.sum().sum(), delta=0.01
        )

    @unittest.skip
    def test_plot_equity_curve(self):
        (strategies_benchmark, strategies_test_output) = self._generate_inputs_compare()
        compare_trading_strategies = CompareTradingAlgorithms(
            strategies_benchmark=strategies_benchmark,
            strategies_test_output=strategies_test_output,
        )
        output_plot_df, fig = compare_trading_strategies.plot_equity_curve(
            plot_equity_column_score_enum=ScoreEnum.unrealized_pnl,
            metrics_equity_column_score_enum=ScoreEnum.unrealized_pnl,
        )
        plt.show()
        self.assertIsNotNone(output_plot_df)
        self.assertIsNotNone(fig)
        self.assertTrue(len(output_plot_df) > 0)

    @unittest.skip
    def test_plot_results_compare(self):
        days = 3
        compare_trading_statc = CompareStatisticallyTradingAlgorithms()
        for day in range(days):
            (
                strategies_benchmark,
                strategies_test_output,
            ) = self._generate_inputs_compare()
            if day == 0:
                strategy_to_none = list(strategies_test_output.keys())[0]
                strategies_test_output[strategy_to_none] = pd.DataFrame()

            compare_trading_strategies = CompareTradingAlgorithms(
                strategies_benchmark=strategies_benchmark,
                strategies_test_output=strategies_test_output,
            )
            results_output_dict = compare_trading_strategies.get_results(
                equity_column_score_enum=ScoreEnum.unrealized_pnl
            )
            compare_trading_statc.add_results_compare(results_output_dict)

        (sharpe_df, fig) = CompareTradingAlgorithms.plot_results_compare(
            score_enum=ScoreEnum.sharpe,
            algorithm_info_color=compare_trading_strategies.get_algorithm_info_color(),
        )
        plt.show()
        self.assertIsNotNone(sharpe_df)
        self.assertIsNotNone(fig)


if __name__ == '__main__':
    unittest.main()
