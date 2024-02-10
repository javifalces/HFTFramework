import copy
import pandas as pd
from utils.pandas_utils.dataframe_utils import join_two_timeseries_different_index_ffill


class ColumnToCompareEnum:
    historicalTotalPnl = 'historicalTotalPnl'
    historicalRealizedPnl = 'historicalRealizedPnl'


def compare_results_algorithms(
        backtest_test_output: list,
        column_to_compare=ColumnToCompareEnum.historicalTotalPnl,
        resample='1Min',
):
    total_pnl = None
    legend = []
    for backtest_test in backtest_test_output:
        name_output = list(backtest_test.keys())[0]
        backtest_result_test = backtest_test[name_output]

        backtest_result_test_indexed = copy.copy(backtest_result_test)
        backtest_result_test_indexed.set_index('date', inplace=True)

        series = backtest_result_test_indexed[column_to_compare]

        df = pd.DataFrame(series).rename(columns={column_to_compare: name_output})
        if total_pnl is None:
            total_pnl = df
        else:
            total_pnl = join_two_timeseries_different_index_ffill(total_pnl, df)

        legend.append(name_output)

    if resample is not None:
        total_pnl = total_pnl.resample(resample).last()

    return total_pnl
