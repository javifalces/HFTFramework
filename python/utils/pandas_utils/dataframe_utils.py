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
                table_2_columns = pd.concat([table_2_columns, column], axis=1)
        if len(table_2_columns) > 0:
            output_df = table_1.join(table_2[table_2_columns])

    return output_df


def join_by_row(table_1: pd.DataFrame, table_2: pd.DataFrame) -> pd.DataFrame:
    # df1 = df1.append(df2)
    # df1 = pd.concat([df1, df2], axis=0)
    assert table_1.shape[1] == table_2.shape[1]
    output_df = table_1.copy()
    output_df = pd.concat([output_df, table_2], axis=0)
    return output_df


def join_two_timeseries_different_index_ffill(
        table_1: pd.DataFrame, table_2: pd.DataFrame
):
    concat_df = pd.concat([table_1, table_2], axis=1)
    concat_df.index = pd.to_datetime(concat_df.index)

    concat_df = concat_df.sort_index()
    concat_df = concat_df.ffill().fillna(0)
    return concat_df


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

    rs = 0.5 * log_hl ** 2 - (2 * math.log(2) - 1) * log_co ** 2
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


def transform_dataframe_index_plot(
        df_in: pd.DataFrame, format='%Y/%m/%d  %H:%M:%s'
) -> pd.DataFrame:
    '''
    Transforms df_in datetime index in a index of strings to avoid blank spaces in plot
    Parameters
    ----------
    df_in

    Returns
    -------

    '''
    df_out = copy.copy(df_in)
    df_out = df_out.rename_axis('index').reset_index()

    time_index = df_out['index']
    time_str_index = time_index.dt.strftime(format)
    df_out['index'] = time_str_index
    df_out.set_index('index', inplace=True)
    return df_out


def align_index(origin_df: pd.DataFrame, target_df: pd.DataFrame) -> pd.DataFrame:
    '''

    Parameters
    ----------
    origin_df we are going to align origin_df index to target_df index
    target_df the index we want to align to

    Returns origin_df aligned to target_df index
    -------

    '''
    closest_series = origin_df.index.searchsorted(target_df.index) - 1
    closest_series = closest_series[closest_series < len(origin_df)]
    output = pd.DataFrame(
        origin_df.values[closest_series, :],
        index=target_df.index,
        columns=origin_df.columns,
    )
    output.fillna(method="ffill", inplace=True)
    return output
