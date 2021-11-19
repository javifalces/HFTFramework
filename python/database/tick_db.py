import datetime
from pathlib import Path

import pandas as pd
import pyarrow.parquet as pq
import os
from glob import glob
import numpy as np
from configuration import PARQUET_PATH_DB
from database.candle_generation import *
import pyarrow as pa


# root_db_path = rf'\\nas\home\lambda_data'


# D:\javif\Coding\cryptotradingdesk\data\type=trade\instrument=btceur_binance\date=20201204
def get_microprice(depth_df):
    volumes = depth_df['askQuantity0']+ depth_df['bidQuantity0']
    return depth_df['askPrice0']*(depth_df['askQuantity0']/volumes)+depth_df['bidPrice0']*(depth_df['bidQuantity0']/volumes)

def get_imbalance(depth_df,max_depth:int=5):
    total_ask_vol=None
    total_bid_vol=None
    for market_horizon_i in range(max_depth):
        if total_ask_vol is None:
            total_ask_vol = depth_df['askQuantity%d' % market_horizon_i]
        else:
            total_ask_vol += depth_df['askQuantity%d'%market_horizon_i]

        if total_bid_vol is None:
            total_bid_vol = depth_df['bidQuantity%d' % market_horizon_i]
        else:
            total_bid_vol += depth_df['bidQuantity%d' % market_horizon_i]
    imbalance = (total_bid_vol-total_ask_vol)/(total_bid_vol+total_ask_vol)
    return imbalance


class TickDB:
    default_start_date = datetime.datetime.today() - datetime.timedelta(days=7)
    default_end_date = datetime.datetime.today()
    FX_MARKETS = ['darwinex', 'metatrader', 'oanda']

    def __init__(self, root_db_path: str = PARQUET_PATH_DB) -> None:
        self.base_path = root_db_path

        self.date_str_format = '%Y%m%d'

    def get_all_data(self, instrument_pk: str,
                          start_date: datetime.datetime = default_start_date,
                          end_date: datetime.datetime = default_end_date)-> pd.DataFrame:
        depth_df = self.get_depth(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date)
        trades_df = self.get_trades(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date)
        candles_df = self.get_candles_time(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date)

        depth_df_2 = depth_df.reset_index()
        depth_df_2['type'] = 'depth'

        trades_df_2 = trades_df.reset_index()
        trades_df_2['type'] = 'trade'

        candles_df.columns = ['_'.join(col).strip() for col in candles_df.columns.values]
        candles_df_2 = candles_df.reset_index()
        candles_df_2['type'] = 'candle'


        backtest_data = pd.concat([depth_df_2, trades_df_2,candles_df_2])

        backtest_data.set_index(keys='date', inplace=True)
        backtest_data.sort_index(inplace=True)
        backtest_data.fillna(method='ffill', inplace=True)
        # backtest_data.dropna(inplace=True)
        return backtest_data

    # def get_all_trades(self, instrument_pk: str):
    #     type_data = 'trade'
    #     path_trades=rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"
    #     return pd.read_parquet(path_trades)
    #
    # def get_all_depth(self, instrument_pk: str):
    #     type_data = 'depth'
    #     return pd.read_parquet(rf"{self.base_path}\type={type_data}\instrument={instrument_pk}")

    def get_all_instruments(self, type_str: str = 'trade')->list:
        source_path = rf"{self.base_path}\type={type_str}"
        all_folders = glob(source_path + "/*")
        instruments = []
        for folder in all_folders:
            instrument = folder.split("instrument=")[-1]
            instruments.append(instrument)
        return instruments

    def get_all_dates(self, type_str: str, instrument_pk: str)->list:
        source_path = rf"{self.base_path}\type={type_str}\instrument={instrument_pk}"
        all_folders = glob(source_path + "/*")
        dates = []
        for folder in all_folders:
            date_str = folder.split("date=")[-1]
            date = datetime.datetime.strptime(date_str, self.date_str_format)
            dates.append(date)
        return dates

    def is_fx_instrument(self, instrument_pk: str) -> bool:
        market_instrument = instrument_pk.split('_')[-1].lower()
        return market_instrument in self.FX_MARKETS

    def get_depth(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
    ):
        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'depth'
        source_path = rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"
        if not os.path.isdir(source_path):
            print(f'DOESNT EXIST  DEPTH {source_path}')

        print(
            "downloading %s %s from %s to %s"
            % (instrument_pk, type_data, start_date_str, end_date_str)
        )
        dataset = pq.ParquetDataset(
            source_path,
            filters=[('date', '>=', start_date_str), ('date', '<=', end_date_str)],
        )
        table = dataset.read()
        df = table.to_pandas()

        df['date'] = pd.to_datetime(df.index * 1000000)
        if not self.is_fx_instrument(instrument_pk=instrument_pk):
            df.dropna(inplace=True)

        df.set_index('date', inplace=True)

        # add basic indicators
        df['midprice'] = (df['askPrice0'] + df['bidPrice0']) / 2
        df['spread'] = (df['askPrice0'] - df['bidPrice0']).abs()
        return df

    def get_trades(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
    ):
        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'trade'
        source_path = rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"

        if not os.path.isdir(source_path):
            print(f'DOESNT EXIST  TRADE {source_path}')

        print(
            "downloading %s %s from %s to %s"
            % (instrument_pk, type_data, start_date_str, end_date_str)
        )
        dataset = pq.ParquetDataset(
            source_path,
            filters=[('date', '>=', start_date_str), ('date', '<=', end_date_str)],
        )
        table = dataset.read()
        df = table.to_pandas()
        df['date'] = pd.to_datetime(df.index * 1000000)
        df.dropna(inplace=True)
        df.set_index('date', inplace=True)
        return df


    def _persist_candles(self,source_path:str,df:pd.DataFrame,start_date:datetime.datetime, end_date: datetime.datetime):

        #ignore warnings
        import warnings
        from pandas.core.common import SettingWithCopyWarning
        warnings.filterwarnings(category=SettingWithCopyWarning,action="ignore")


        day_to_persist=start_date.replace(hour=0,minute=0,second=0,microsecond=0)

        output = None
        while day_to_persist<=end_date:
            day_to_persist_str = day_to_persist.strftime(self.date_str_format)
            complete_path = source_path+os.sep+"date="+day_to_persist_str
            next_day = day_to_persist+datetime.timedelta(days=1)
            df_to_persist = df.loc[day_to_persist:next_day]
            if len(df_to_persist)>0:
                Path(complete_path).mkdir(parents=True, exist_ok=True)
                file_path = complete_path + os.sep + 'data.parquet'
                if os.path.exists(file_path):
                    os.remove(file_path)
                # error schema
                df_to_persist.to_parquet(file_path,compression='GZIP')

                # table = pa.Table.from_pandas(df_to_persist)
                # pq.write_table(table, complete_path + os.sep + 'data.parquet',compression='GZIP')


            day_to_persist=next_day


    def _check_all_candles_exist(self, df: pd.DataFrame, start_date: datetime.datetime,
                         end_date: datetime.datetime):
        day_to_persist = start_date

        def is_business_day(date):
            return bool(len(pd.bdate_range(date, date)))

        if df.index[0]-datetime.timedelta(days=1)>start_date or df.index[-1]+datetime.timedelta(days=1)<end_date :
            print(f"some day is missing on candles between {start_date} and {end_date}  -> df from {df.index[0]} and {df.index[-1]}")
            raise Exception(f"some day is missing on candles between {start_date} and {end_date}  -> df from {df.index[0]} and {df.index[-1]}")


    def get_candles_midprice_time(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            resolution: str = 'MIN', num_units: int = 1, is_error_call: bool = False
    ):
        '''

        :param instrument_pk:
        :param start_date:
        :param end_date:
        :param resolution: (str) Resolution type ('D', 'H', 'MIN', 'S')
        :param num_units: (int) Number of resolution units (3 days for example, 2 hours)
        :param is_error_call:
        :return:
        '''

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_midpricetime_%s%d' % (resolution, num_units)
        source_path = rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"
        if not os.path.isdir(source_path):
            print(f'DOESNT EXIST  candle_midpricetime {source_path}')
        try:
            print(
                "downloading %s %s from %s to %s"
                % (instrument_pk, type_data, start_date_str, end_date_str)
            )

            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<=', end_date_str)],
            )
            table = dataset.read()
            df = table.to_pandas()

            self._check_all_candles_exist(
                df=df, start_date=start_date, end_date=end_date
            )
        except Exception as e:
            if is_error_call:
                raise e
            depth_df = self.get_depth(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date)
            df = generate_candle_time(df=depth_df, resolution=resolution, num_units=num_units)

            self._persist_candles(df=df, start_date=start_date, end_date=end_date, source_path=source_path)
            return self.get_candles_midprice_time(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date,
                                                  resolution=resolution, num_units=num_units, is_error_call=True)

        return df

    def get_candles_time(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            resolution: str = 'MIN', num_units: int = 1, is_error_call: bool = False
    ):
        '''

        :param instrument_pk:
        :param start_date:
        :param end_date:
        :param resolution: (str) Resolution type ('D', 'H', 'MIN', 'S')
        :param num_units: (int) Number of resolution units (3 days for example, 2 hours)
        :param is_error_call:
        :return:
        '''

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_time_%s%d' % (resolution, num_units)
        source_path = rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"
        if not os.path.isdir(source_path):
            print(f'DOESNT EXIST  candle_time {source_path}')

        try:
            print(
                "downloading %s %s from %s to %s"
                % (instrument_pk, type_data, start_date_str, end_date_str)
            )

            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<=', end_date_str)],
            )
            table = dataset.read()
            df = table.to_pandas()


            self._check_all_candles_exist(
                df=df, start_date=start_date, end_date=end_date
            )
        except Exception as e:
            if is_error_call:
                raise e
            trades_df = self.get_trades(
                instrument_pk=instrument_pk, start_date=start_date, end_date=end_date
            )
            df = generate_candle_time(df=trades_df, resolution=resolution,num_units=num_units)

            self._persist_candles(df=df,start_date=start_date,end_date=end_date,source_path=source_path)
            return self.get_candles_time(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date,
                                         resolution=resolution, num_units=num_units,is_error_call=True)
        return df

    def get_candles_tick(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            number_of_ticks:int=100,is_error_call:bool=False
    ):
        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_tick_%d' % (number_of_ticks)
        source_path = rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"
        try:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<=', end_date_str)],
            )
            table = dataset.read()
            df = table.to_pandas()

            self._check_all_candles_exist(
                df=df, start_date=start_date, end_date=end_date
            )

        except Exception as e:
            if is_error_call:
                raise e

            trades_df = self.get_trades(
                instrument_pk=instrument_pk, start_date=start_date, end_date=end_date
            )
            df = generate_candle_tick(df=trades_df, number_of_ticks=number_of_ticks)
            self._persist_candles(df=df,start_date=start_date,end_date=end_date,source_path=source_path)
            return self.get_candles_tick(instrument_pk=instrument_pk,start_date=start_date,end_date=end_date,number_of_ticks=number_of_ticks,is_error_call=True)

        return df

    def get_candles_volume(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            volume:float=100,is_error_call:bool=False
    ):
        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_volume_%f' % (volume)
        source_path = rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"
        try:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<=', end_date_str)],
            )
            table = dataset.read()
            df = table.to_pandas()


            # self._check_all_candles_exist(
            #     df=df, start_date=start_date, end_date=end_date
            # )

        except Exception as e:
            if is_error_call:
                raise e

            trades_df = self.get_trades(
                instrument_pk=instrument_pk, start_date=start_date, end_date=end_date
            )
            df = generate_candle_volume(df=trades_df, volume=volume)
            self._persist_candles(df=df,start_date=start_date,end_date=end_date,source_path=source_path)
            return self.get_candles_volume(instrument_pk=instrument_pk,start_date=start_date,end_date=end_date,volume=volume,is_error_call=True)

        return df
    def get_candles_dollar_value(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            dollar_value:float=1000,is_error_call:bool=False
    ):
        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_dollar_value_%d' % (dollar_value)
        source_path = rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"
        try:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<=', end_date_str)],
            )
            table = dataset.read()
            df = table.to_pandas()

            self._check_all_candles_exist(
                df=df, start_date=start_date, end_date=end_date
            )

        except Exception as e:
            if is_error_call:
                raise e

            trades_df = self.get_trades(
                instrument_pk=instrument_pk, start_date=start_date, end_date=end_date
            )
            df = generate_candle_dollar_value(df=trades_df, volume=volume)
            self._persist_candles(df=df,start_date=start_date,end_date=end_date,source_path=source_path)
            return self.get_candles_dollar_value(instrument_pk=instrument_pk,start_date=start_date,end_date=end_date,dollar_value=dollar_value,is_error_call=True)

        return df

if __name__ == '__main__':
    tick = TickDB()
    instrument_pk = 'btcusdt_binance'

    # instruments = tick.get_all_instruments()
    # dates = tick.get_all_dates(type_str='depth', instrument_pk=instrument_pk)
    # # trades_df_all = tick.get_all_trades(instrument_pk=LambdaInstrument.btcusdt_binance)
    # trades_df = tick.get_trades(instrument_pk=instrument_pk)
    # depth_df = tick.get_depth(instrument_pk=instrument_pk)
    # all = tick.get_all_data(instrument_pk=instrument_pk,start_date=datetime.datetime(year=2020, day=7, month=12),end_date=datetime.datetime(year=2020, day=7, month=12))
    # candle_time = tick.get_candles_time(instrument_pk=instrument_pk,start_date=datetime.datetime(year=2020, day=7, month=12),end_date=datetime.datetime(year=2020, day=7, month=12))
    # candle_tick = tick.get_candles_tick(instrument_pk=instrument_pk,
    #                                     start_date=datetime.datetime(year=2020, day=7, month=12),
    #                                     end_date=datetime.datetime(year=2020, day=7, month=12),
    #                                     number_of_ticks=5
    #                                     )
    #
    # candle_volume = tick.get_candles_volume(instrument_pk=instrument_pk,
    #                                     start_date=datetime.datetime(year=2020, day=7, month=12),
    #                                     end_date=datetime.datetime(year=2020, day=7, month=12),
    #                                     volume=500
    #                                     )

    candle_dollar_volume = tick.get_candles_dollar_value(instrument_pk=instrument_pk,
                                        start_date=datetime.datetime(year=2020, day=7, month=12),
                                        end_date=datetime.datetime(year=2020, day=7, month=12),
                                        dollar_value=1000
                                        )