import datetime

import mlfinlab
import pandas as pd
import numpy as np


def generate_candle_time(
        df, resolution='MIN', num_units=1, batch_size: int = 2e7
) -> pd.DataFrame:
    '''

    :param df:
    :param resolution: (str) Resolution type ('D', 'H', 'MIN', 'S')
    :param num_units: (int) Number of resolution units (3 days for example, 2 hours)
    :param batch_size: (int) The number of rows per batch. Less RAM = smaller batch size.
    :return:
    '''
    output_df = mlfinlab.data_structures.get_time_bars(
        file_path_or_df=df.reset_index(),
        resolution=resolution,
        num_units=num_units,
        batch_size=batch_size,
        verbose=False,
    )
    # output_df['date'] = pd.to_datetime(output_df['date_time']* 1000000000)
    # fillnas like in candle publisher
    last_close = output_df['close'].ffill().shift(1)
    for column in ['open', 'high', 'low', 'close']:
        output_df[column] = output_df[column].fillna(last_close)

    output_df['volume'] = output_df['volume'].fillna(0.0)
    output_df['cum_dollar_value'] = output_df['cum_dollar_value'].fillna(0.0)
    output_df['cum_ticks'] = output_df['cum_ticks'].fillna(0.0)

    output_df['datetime'] = pd.to_datetime(output_df['date_time'] * 1000000000)
    # if resolution == 'D':
    #     output_df['datetime'] = output_df['datetime'] - datetime.timedelta(days=num_units)

    output_df.set_index('datetime', inplace=True)
    return output_df


def generate_candle_tick(df, number_of_ticks=5) -> pd.DataFrame:
    output_df = mlfinlab.data_structures.get_tick_bars(
        file_path_or_df=df.reset_index(), threshold=number_of_ticks
    )
    output_df.set_index('date_time', inplace=True)
    return output_df


def generate_candle_volume(df, volume=5000):
    output_df = mlfinlab.data_structures.get_volume_bars(
        file_path_or_df=df.reset_index(), threshold=volume
    )
    output_df.set_index('date_time', inplace=True)
    return output_df


def generate_candle_dollar_value(df, dollar_value=5000):
    output_df = mlfinlab.data_structures.get_dollar_bars(
        file_path_or_df=df.reset_index(), threshold=dollar_value
    )
    output_df.set_index('date_time', inplace=True)
    return output_df
