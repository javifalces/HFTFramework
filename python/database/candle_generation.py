import datetime
import os

import mlfinlab
import pandas as pd
import numpy as np

from utils.pandas_utils.dataframe_utils import reduce_memory_usage


def _add_candle_times(df: pd.DataFrame, period_seconds: int = None) -> pd.DataFrame:
    if df.empty:
        df['openTimestamp'] = pd.Series(index=df.index, dtype='int64')
        df['closeTimestamp'] = pd.Series(index=df.index, dtype='int64')
        return df

    if 'openTimestamp' not in df.columns and 'openTime' in df.columns:
        df['openTimestamp'] = df['openTime']
    if 'closeTimestamp' not in df.columns and 'closeTime' in df.columns:
        df['closeTimestamp'] = df['closeTime']
    close_timestamps = df['closeTimestamp'] if 'closeTimestamp' in df.columns else (
        df['date_time'] if 'date_time' in df.columns else pd.Series(df.index, index=df.index)
    )
    if 'openTimestamp' not in df.columns:
        if period_seconds is not None:
            if pd.api.types.is_datetime64_any_dtype(close_timestamps):
                open_timestamps = close_timestamps - pd.to_timedelta(period_seconds, unit='s')
            else:
                open_timestamps = close_timestamps - period_seconds
        else:
            open_timestamps = close_timestamps.shift(1).fillna(close_timestamps.iloc[0])
        df['openTimestamp'] = open_timestamps
    if 'closeTimestamp' not in df.columns:
        df['closeTimestamp'] = close_timestamps
    if 'date_time' in df.columns:
        df['date_time'] = df['openTimestamp']
    if 'timestamp' in df.columns:
        df['timestamp'] = df['openTimestamp']
    if df.index.name == 'date_time':
        df.index = pd.Index(df['openTimestamp'], name='date_time')
    elif df.index.name == 'datetime':
        open_index = df['openTimestamp']
        if not pd.api.types.is_datetime64_any_dtype(open_index):
            open_index = pd.to_datetime(open_index, unit='s')
        df.index = pd.DatetimeIndex(open_index, name='datetime')
    df.drop(columns=['openTime', 'closeTime'], errors='ignore', inplace=True)
    return df


def _resolution_seconds(resolution: str, num_units: int) -> int:
    units_to_seconds = {'D': 86400, 'H': 3600, 'MIN': 60, 'S': 1}
    return units_to_seconds[resolution] * num_units


def generate_candle_time_legacy(
        df, resolution='MIN', num_units=1, batch_size: int = int(os.getenv("CANDLES_BATCH_SIZE", 10000))
) -> pd.DataFrame:
    '''

    :param df:
    :param resolution: (str) Resolution type ('D', 'H', 'MIN', 'S')
    :param num_units: (int) Number of resolution units (3 days for example, 2 hours)
    :param batch_size: (int) The number of rows per batch. Less RAM = smaller batch size.
    :return: dataframe of the candles where datetime is the closing time
    '''
    df = reduce_memory_usage(df)
    # reduce memory size before processing
    output_df = mlfinlab.data_structures.get_time_bars(
        file_path_or_df=df.reset_index(),
        resolution=resolution,
        num_units=num_units,
        batch_size=batch_size,
        verbose=False,
    )
    output_df = reduce_memory_usage(output_df)
    # output_df['date'] = pd.to_datetime(output_df['date_time']* 1000000000)
    # fillnas like in candle publisher
    last_close = output_df['close'].ffill().shift(1)
    for column in ['open', 'high', 'low', 'close']:
        output_df[column] = output_df[column].fillna(last_close)

    output_df['volume'] = output_df['volume'].fillna(0.0)
    output_df['cum_dollar_value'] = output_df['cum_dollar_value'].fillna(0.0)
    output_df['cum_ticks'] = output_df['cum_ticks'].fillna(0.0)

    # set datetime as timestamp[ns]
    output_df = _add_candle_times(output_df, _resolution_seconds(resolution, num_units))
    output_df['datetime'] = pd.to_datetime(output_df['date_time'] * 1000000000, unit='ns')
    # if resolution == 'D':
    #     output_df['datetime'] = output_df['datetime'] - datetime.timedelta(days=num_units)

    output_df.set_index('datetime', inplace=True)
    output_df.sort_index(inplace=True)
    return output_df


def generate_candle_tick(df, number_of_ticks=5) -> pd.DataFrame:
    df = reduce_memory_usage(df)
    output_df = mlfinlab.data_structures.get_tick_bars(
        file_path_or_df=df.reset_index(), threshold=number_of_ticks
    )
    output_df.set_index('date_time', inplace=True)
    return _add_candle_times(output_df)


def generate_candle_volume(df, volume=5000):
    df = reduce_memory_usage(df)  # reduce memory size before processing
    output_df = mlfinlab.data_structures.get_volume_bars(
        file_path_or_df=df.reset_index(), threshold=volume
    )
    output_df.set_index('date_time', inplace=True)
    return _add_candle_times(output_df)


def generate_candle_dollar_value(df, dollar_value=5000):
    df = reduce_memory_usage(df)  # reduce memory size before processing
    output_df = mlfinlab.data_structures.get_dollar_bars(
        file_path_or_df=df.reset_index(), threshold=dollar_value
    )
    output_df.set_index('date_time', inplace=True)
    return _add_candle_times(output_df)


def generate_candle_time(
        df, resolution='MIN', num_units=1
) -> pd.DataFrame:
    """
    Generate time candles using polars library.

    :param df: Input DataFrame with columns ['date_time', 'midprice', askQuantity0-4, bidQuantity0-4]
    :param resolution: (str) Resolution type ('D', 'H', 'MIN', 'S')
    :param num_units: (int) Number of resolution units
    :return: DataFrame of candles with datetime as index
    """
    import polars as pl

    # Convert to polars DataFrame
    pdf = df.reset_index()
    if 'date_time' not in pdf.columns:
        pdf = pdf.rename(columns={pdf.columns[0]: 'date_time'})
    pl_df = pl.from_pandas(pdf)
    pl_df = pl_df.with_columns([
        pl.col('date_time').cast(pl.Datetime('ns'))
    ])

    # Create volume column as sum of askQuantity0-4 and bidQuantity0-4
    ask_cols = [f"askQuantity{i}" for i in range(5)]
    bid_cols = [f"bidQuantity{i}" for i in range(5)]
    volume_cols = ask_cols + bid_cols
    # volume_series = pl_df[volume_cols].sum_horizontal().alias("volume")
    volume_series = pl_df.select([
        pl.sum_horizontal([pl.col(c).fill_null(0).cast(pl.Float64).fill_nan(0) for c in volume_cols])
        .alias("volume")
    ])
    # volume_series = pl_df.select([         pl.sum_horizontal([pl.col(c) for c in volume_cols])         .alias("volume")     ])
    # set in pl_df
    pl_df = pl_df.with_columns(volume_series)

    # Build rule string for resample
    rule_map = {'D': 'd', 'H': 'h', 'MIN': 'm', 'S': 's'}
    rule = f"{num_units}{rule_map[resolution]}"

    # Resample and aggregate
    agg_df = (
        pl_df.group_by_dynamic('date_time', every=rule, closed='right', label='right').agg(
            pl.col('midprice').first().alias('open'),
            pl.col('midprice').max().alias('high'),
            pl.col('midprice').min().alias('low'),
            pl.col('midprice').last().alias('close'),
            pl.col('volume').sum().alias('volume'),
            (pl.col('midprice') * pl.col('volume')).sum().alias('cum_dollar_value'),
            pl.len().alias('cum_ticks')
        )
        .sort('date_time')
    )

    # Fill NaNs as in generate_candle_time
    out_df = agg_df.to_pandas()
    last_close = out_df['close'].ffill().shift(1)
    for column in ['open', 'high', 'low', 'close']:
        out_df[column] = out_df[column].fillna(last_close)
    out_df['volume'] = out_df['volume'].fillna(0.0)
    out_df['cum_dollar_value'] = out_df['cum_dollar_value'].fillna(0.0)
    out_df['cum_buy_volume'] = 0.0
    out_df['tick_num'] = out_df['cum_ticks'].fillna(0.0)
    out_df['tick_num'] = out_df['tick_num'].cumsum()

    out_df['date_time'] = pd.to_numeric(out_df['date_time']) / 1E9
    out_df['date_time'] = out_df['date_time'].astype('int64', copy=False, errors='ignore')
    out_df = _add_candle_times(out_df, _resolution_seconds(resolution, num_units))
    out_df['datetime'] = pd.to_datetime(out_df['date_time'] * 1E9)
    out_df.set_index('datetime', inplace=True)
    out_df.sort_index(inplace=True)
    out_df = reduce_memory_usage(out_df)  # reduce memory size before processing
    return out_df


if __name__ == '__main__':
    # test generate_candle_time
    from database.tick_db import TickDB

    tick = TickDB()
    instrument_pk = 'btceur_kraken'
    start_date = datetime.datetime(day=10, month=5, year=2024)
    end_date = datetime.datetime(day=10, month=5, year=2024)
    depth_df = tick.get_depth(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date, first_hour=7,
                              last_hour=15)

    candles_df = generate_candle_time(depth_df, resolution='MIN', num_units=5)
    print(candles_df.head())
