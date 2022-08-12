import pandas as pd
import copy


def join_by_columns(table_1: pd.DataFrame, table_2: pd.DataFrame) -> pd.DataFrame:
    # df1 = df1.join(df2)
    # df1 = pd.concat([df1, df2], axis=1)
    assert table_1.shape[0] == table_2.shape[0]
    try:
        output_df = copy.copy(table_1)
        output_df = output_df.join(table_2)
    except:
        output_df = copy.copy(table_1)
        table_2_columns = []
        for column in list(table_2.columns):
            if column not in table_1.columns:
                table_2_columns.append(column)
        if len(table_2_columns) > 0:
            output_df = table_1.join(table_2[table_2_columns])

    return output_df


def join_by_row(table_1: pd.DataFrame, table_2: pd.DataFrame) -> pd.DataFrame:
    # df1 = df1.append(df2)
    # df1 = pd.concat([df1, df2], axis=0)
    assert table_1.shape[1] == table_2.shape[1]
    output_df = copy.copy(table_1)
    output_df = output_df.append(table_2)
    return output_df


def join_two_timeseries_different_index_ffill(
    table_1: pd.DataFrame, table_2: pd.DataFrame
):
    # TODO
    return None


def garman_klass_volatility(candles_df: pd.DataFrame):
    high = candles_df['high']
    low = candles_df['low']
    open = candles_df['open']
    close = candles_df['close']
    return garman_klass_volatility(high=high, low=low, open=open, close=close)


def garman_klass_volatility(
    high: pd.Series,
    low: pd.Series,
    open: pd.Series,
    close: pd.Series,
    trading_periods=None,
    window=None,
    clean=True,
):
    import numpy as np
    import math

    log_hl = (high / low).apply(np.log)
    log_co = (close / open).apply(np.log)

    rs = 0.5 * log_hl**2 - (2 * math.log(2) - 1) * log_co**2
    return_last = False

    if trading_periods is None and window is None:
        return_last = True

    if trading_periods is None:
        trading_periods = 250

    if window is None:
        window = len(high)

    def f(v):
        return (trading_periods * v.mean()) ** 0.5

    result = rs.rolling(window=window, center=False).apply(func=f)
    if clean:
        output = result.dropna()
    else:
        output = result
    if return_last:
        output = result.iloc[-1]
    return output
