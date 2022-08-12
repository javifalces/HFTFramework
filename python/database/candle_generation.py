import datetime

import mlfinlab
import pandas as pd
import numpy as np


def generate_candle_time(df, resolution='MIN', num_units=1) -> pd.DataFrame:
    '''

    :param df:
    :param resolution: (str) Resolution type ('D', 'H', 'MIN', 'S')
    :param num_units: (int) Number of resolution units (3 days for example, 2 hours)
    :return:
    '''
    output_df = mlfinlab.data_structures.get_time_bars(
        file_path_or_df=df.reset_index(), resolution=resolution, num_units=num_units
    )
    output_df['datetime'] = pd.to_datetime(output_df['date_time'] * 1000000000)
    if resolution == 'D':
        output_df['datetime'] = output_df['datetime'] - datetime.timedelta(days=1)

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
