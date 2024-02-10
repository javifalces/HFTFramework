import datetime
import unittest

from test.utils.test_util import set_test_data_path
from trading_algorithms.market_making.avellaneda_stoikov import (
    AvellanedaStoikov,
    AvellanedaStoikovParameters,
)
from trading_algorithms.market_making.constant_spread import (
    ConstantSpread,
    ConstantSpreadParameters,
)
from trading_algorithms.benchmark_manager.compare_trading_algorithms import (
    CompareTradingAlgorithmsLauncher,
    CompareTradingAlgorithms,
)
from trading_algorithms.score_enum import ScoreEnum


@unittest.skip("need java package to run")
class CompareBacktestLauncherTest(unittest.TestCase):
    set_test_data_path()

    avellaneda_stoikov = AvellanedaStoikov(
        algorithm_info='AvellanedaStoikovTest1',
        parameters={
            AvellanedaStoikovParameters.risk_aversion: 0.9,
            AvellanedaStoikovParameters.midprice_period_seconds: 1,
            AvellanedaStoikovParameters.midprice_period_window: 15,
            AvellanedaStoikovParameters.seconds_change_k: 5,
        },
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

    def test_normal_launcher(self):
        start_date = datetime.datetime(2022, 1, 15, 11)
        end_date = datetime.datetime(2022, 1, 15, 12)
        instrument_pk = 'btcusdt_binance'

        compare_trading_launcher = CompareTradingAlgorithmsLauncher(
            algorithms_list=self.algorithms_list,
            instrument_pk=instrument_pk,
            start_date_test=start_date,
            end_date_test=end_date,
        )
        result_compare = compare_trading_launcher.launch_test()

        self.assertIsNotNone(result_compare)
        self.assertEqual(len(self.algorithms_list), len(result_compare))

    def test_launcher_and_compare(self):
        start_date = datetime.datetime(2022, 1, 15, 11)
        end_date = datetime.datetime(2022, 1, 15, 12)
        instrument_pk = 'btcusdt_binance'

        compare_trading_launcher = CompareTradingAlgorithmsLauncher(
            algorithms_list=self.algorithms_list,
            instrument_pk=instrument_pk,
            start_date_test=start_date,
            end_date_test=end_date,
        )
        result_compare = compare_trading_launcher.launch_test()

        self.assertIsNotNone(result_compare)
        from trading_algorithms.benchmark_manager.compare_trading_algorithms import (
            CompareTradingAlgorithms,
        )

        strategies_benchmark = {
            self.avellaneda_stoikov: True,
            self.constant_spread2: False,
        }

        compare_trading_algorithm = CompareTradingAlgorithms(
            strategies_benchmark=strategies_benchmark,
            strategies_test_output=result_compare,
        )
        is_benchmark_detect = compare_trading_algorithm.is_benchmark_algorithm(
            self.avellaneda_stoikov.algorithm_info
        )
        self.assertTrue(is_benchmark_detect)

        is_benchmark_detect2 = compare_trading_algorithm.is_benchmark_algorithm(
            self.constant_spread2.algorithm_info
        )
        self.assertFalse(is_benchmark_detect2)

        results_dict = compare_trading_algorithm.get_results(
            equity_column_score_enum=ScoreEnum.unrealized_pnl
        )
        self.assertIsNotNone(results_dict)

    if __name__ == '__main__':
        unittest.main()
