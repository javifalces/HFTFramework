import datetime
from pathlib import Path

import pandas as pd
import numpy as np
import os
from glob import glob

from pandas._libs.tslibs.offsets import BDay

from configuration import LAMBDA_DATA_PATH
from utils.cache_utils import memory_cache
from utils.pandas_utils.dataframe_utils import reduce_memory_usage


# root_db_path = rf'\\nas\home\lambda_data'


# D:\javif\Coding\cryptotradingdesk\data\type=trade\instrument=btceur_binance\date=20201204
def get_microprice(depth_df):
    volumes = depth_df['askQuantity0'] + depth_df['bidQuantity0']
    return depth_df['askPrice0'] * (depth_df['askQuantity0'] / volumes) + depth_df[
        'bidPrice0'
    ] * (depth_df['bidQuantity0'] / volumes)


def get_imbalance(depth_df, max_depth: int = 5):
    total_ask_vol = None
    total_bid_vol = None
    for market_horizon_i in range(max_depth):
        if total_ask_vol is None:
            total_ask_vol = depth_df['askQuantity%d' % market_horizon_i]
        else:
            total_ask_vol += depth_df['askQuantity%d' % market_horizon_i]

        if total_bid_vol is None:
            total_bid_vol = depth_df['bidQuantity%d' % market_horizon_i]
        else:
            total_bid_vol += depth_df['bidQuantity%d' % market_horizon_i]
    imbalance = (total_bid_vol - total_ask_vol) / (total_bid_vol + total_ask_vol)
    return imbalance


class CandleType:
    CANDLE_TIME = 'candle_time'
    CANDLE_MIDPRICE_TIME = 'candle_midpricetime'
    CANDLE_TICK = 'candle_tick'
    CANDLE_VOLUME = 'candle_volume'


class CandleTimeResolution:
    # Resolution type ('D', 'H', 'MIN', 'S')
    MIN = 'MIN'
    HOUR = 'H'
    DAY = 'D'
    SECOND = 'S'


class TickDB:
    default_start_date = datetime.datetime.today() - datetime.timedelta(days=7)
    default_end_date = datetime.datetime.today()
    FX_MARKETS = ['darwinex', 'metatrader', 'oanda']

    def __init__(self, root_db_path: str = LAMBDA_DATA_PATH) -> None:
        self.base_path = root_db_path

        self.date_str_format = '%Y%m%d'

    def get_all_data(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            first_hour: int = None,
            last_hour: int = None,
    ) -> pd.DataFrame:
        depth_df = self.get_depth(
            instrument_pk=instrument_pk,
            start_date=start_date,
            end_date=end_date,
            first_hour=first_hour,
            last_hour=last_hour,
        )
        trades_df = self.get_trades(
            instrument_pk=instrument_pk,
            start_date=start_date,
            end_date=end_date,
            first_hour=first_hour,
            last_hour=last_hour,
        )
        candles_df = self.get_candles_time(
            instrument_pk=instrument_pk,
            start_date=start_date,
            end_date=end_date,
            first_hour=first_hour,
            last_hour=last_hour,
        )

        depth_df_2 = depth_df.reset_index()
        depth_df_2['type'] = 'depth'

        trades_df_2 = trades_df.reset_index()
        trades_df_2['type'] = 'trade'

        candles_df.columns = [
            '_'.join(col).strip() for col in candles_df.columns.values
        ]
        candles_df_2 = candles_df.reset_index()
        candles_df_2['type'] = 'candle'

        backtest_data = pd.concat([depth_df_2, trades_df_2, candles_df_2])

        backtest_data.set_index(keys='date', inplace=True)
        backtest_data.sort_index(inplace=True)
        backtest_data.ffill(inplace=True)
        # backtest_data.dropna(inplace=True)
        return backtest_data

    # def get_all_trades(self, instrument_pk: str):
    #     type_data = 'trade'
    #     path_trades=rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"
    #     return pd.read_parquet(path_trades)
    #
    # def get_all_depth(self, instrument_pk: str):
    #     type_data = 'depth'
    #     return pd.read_parquet(rf"{self.base_path}/type={type_data}/instrument={instrument_pk}")

    def get_all_instruments(self, type_str: str = 'depth', modified_since=None, currency=None) -> list:
        source_path = rf"{self.base_path}/type={type_str}"
        # source_path = os.path.normpath(source_path)
        all_folders = glob(source_path + "/*")
        if modified_since is not None:
            all_folders = [folder for folder in all_folders if os.path.getmtime(folder) > modified_since.timestamp()]
        instruments = []
        for folder in all_folders:
            instrument = folder.split("instrument=")[-1]
            instruments.append(instrument)
        if currency is not None:
            instruments = [inst for inst in instruments if currency.lower() in inst.lower()]

        return instruments

    def get_all_dates(self, type_str: str, instrument_pk: str) -> list:
        source_path = rf"{self.base_path}/type={type_str}/instrument={instrument_pk}"
        # source_path = os.path.normpath(source_path)
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

    @memory_cache(maxsize=256)
    def get_depth(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            first_hour: int = None,
            last_hour: int = None,
            columns: list = None,
    ):
        '''

        Parameters
        ----------
        instrument_pk
        start_date : included
        end_date :  included
        first_hour
        last_hour
        columns

        Returns
        -------

        '''

        start_date, end_date = self._check_start_date_end_date(start_date, end_date)
        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'depth'
        source_path = rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"

        # source_path = os.path.normpath(source_path)
        if not os.path.isdir(source_path):
            print(f'creating DEPTH {source_path} ...')

        # print(
        #     "querying %s tick_db %s from %s to %s"
        #     % (type_data, instrument_pk, start_date_str, end_date_str)
        # )
        try:
            import pyarrow.parquet as pq

            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', int(start_date_str)), ('date', '<=', int(end_date_str))],
            )
            if columns is None:
                table = dataset.read()
            else:
                table = dataset.read(columns=columns)

            df = table.to_pandas()
        except Exception as e:
            print(
                f"can't read get_depth using parquet {instrument_pk} between {start_date_str} and {end_date_str} => try manual mode using pandas\n{e}"
            )
            df = self._get_manual_pandas(
                instrument_pk=instrument_pk, start_date=start_date, end_date=end_date
            )
        if df is None or len(df) == 0:
            print(
                rf"WARNING: no data found {instrument_pk} between {start_date_str} and {end_date_str} -> None"
            )
            return None
        df = reduce_memory_usage(df)
        if columns is not None:
            df = df.loc[:, columns]

        df = self.create_date_index(df)

        if not self.is_fx_instrument(instrument_pk=instrument_pk):
            df.dropna(inplace=True)

        df.set_index('date', inplace=True)

        # add basic indicators
        df['midprice'] = (df['askPrice0'] + df['bidPrice0']) / 2
        df['spread'] = (df['askPrice0'] - df['bidPrice0']).abs()
        if first_hour is not None and last_hour is not None:
            df = df.between_time(
                start_time=rf"{first_hour}:00", end_time=rf"{last_hour}:00"
            )

        if start_date is not None:
            try:
                df = df[start_date:]
            except Exception as e:
                print(rf"WARNING: start_date={start_date} -> {e}")
        if end_date is not None:
            try:
                df = df[:end_date]
            except Exception as e:
                print(rf"WARNING: end_date={end_date} -> {e}")
        df = reduce_memory_usage(df)
        df.index = pd.to_datetime(df.index)
        df.sort_index(inplace=True)
        return df

    def create_date_index(self, df) -> pd.DataFrame:
        if df.index[0] == 0 and df.index[1] == 1 and 'timestamp' in df.columns:
            df.set_index('timestamp', inplace=True)
        df['date'] = pd.to_datetime(df.index * 1000000)
        return df

    def _check_start_date_end_date(self, start_date, end_date):
        # if isinstance(start_date, datetime.date):
        #     start_date = datetime.datetime.combine(start_date, datetime.datetime.min.time())
        # if isinstance(end_date, datetime.date):
        #     end_date = datetime.datetime.combine(end_date, datetime.datetime.max.time())
        if start_date == end_date:
            # print(rf"WARNING: start_date==end_date={start_date} -> adding 24 hours to end_date and 0 to start_date")
            start_date = datetime.datetime.combine(start_date, datetime.datetime.min.time())
            end_date = datetime.datetime.combine(end_date, datetime.datetime.max.time())

        return start_date, end_date

    @memory_cache(maxsize=256)
    def get_trades(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            first_hour: int = None,
            last_hour: int = None,
    ):
        '''

        Parameters
        ----------
        instrument_pk
        start_date :included
        end_date: included
        first_hour
        last_hour

        Returns
        -------

        '''
        import pyarrow.parquet as pq
        start_date, end_date = self._check_start_date_end_date(start_date, end_date)

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'trade'
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        if not os.path.isdir(source_path):
            print(f'creating TRADE {source_path} ...')

        # print(
        #     "querying %s tick_db %s from %s to %s"
        #     % (type_data, instrument_pk, start_date_str, end_date_str)
        # )
        dataset = pq.ParquetDataset(
            source_path,
            filters=[('date', '>=', int(start_date_str)), ('date', '<=', int(end_date_str))],
        )
        table = dataset.read()
        df = table.to_pandas()
        df = reduce_memory_usage(df)

        df = self.create_date_index(df)

        df.dropna(inplace=True)
        df.set_index('date', inplace=True)
        if first_hour is not None and last_hour is not None:
            df = df.between_time(
                start_time=rf"{first_hour}:00", end_time=rf"{last_hour}:00"
            )

        if start_date is not None:
            df = df[start_date:]
        if end_date is not None:
            df = df[:end_date]
        df = reduce_memory_usage(df)
        df.index = pd.to_datetime(df.index)
        df.sort_index(inplace=True)
        return df

    def _persist_candles(
            self,
            source_path: str,
            df: pd.DataFrame,
            start_date: datetime.datetime,
            end_date: datetime.datetime,
    ):
        day_to_persist = start_date.replace(hour=0, minute=0, second=0, microsecond=0)
        output = None
        while day_to_persist < end_date:
            day_to_persist_str = day_to_persist.strftime(self.date_str_format)
            complete_path = source_path + os.sep + "date=" + day_to_persist_str
            next_day = day_to_persist + datetime.timedelta(days=1)
            df_to_persist = df.loc[
                            day_to_persist: next_day - datetime.timedelta(minutes=1)
                            ]

            len_is_valid = len(df_to_persist) > 0

            if len_is_valid:
                Path(complete_path).mkdir(parents=True, exist_ok=True)
                file_path = complete_path + os.sep + 'data.parquet'
                if os.path.exists(file_path):
                    os.remove(file_path)
                # error schema
                try:
                    print(rf"persisting {file_path} ... from {df_to_persist.index[0]} to {df_to_persist.index[-1]}... ")
                    df_to_persist.to_parquet(file_path, use_dictionary=False)
                except Exception as e:
                    df_to_persist.to_parquet(
                        file_path, engine='fastparquet', use_dictionary=False
                    )

                # table = pa.Table.from_pandas(df_to_persist)
                # pq.write_table(table, complete_path + os.sep + 'data.parquet',compression='GZIP')

            day_to_persist = next_day

    def _ensure_candle_times(self, df: pd.DataFrame, period_seconds: int = None) -> pd.DataFrame:
        from database.candle_generation import _add_candle_times

        return _add_candle_times(df, period_seconds)

    def _check_all_candles_exist(
            self,
            df: pd.DataFrame,
            start_date: datetime.datetime,
            end_date: datetime.datetime,
    ):
        missing_start = df.index[0] - BDay(2) > start_date
        missing_end = df.index[-1] + BDay(1) < end_date

        if missing_start or missing_end:
            if missing_start and missing_end:
                print(
                    f"some day is missing on candles between {start_date} and {end_date}  -> df from first_index:{df.index[0]}>start_date: {start_date} and last_index:{df.index[-1]}< end_date: {end_date}"
                )
            elif missing_start:
                print(
                    f"some day is missing on candles between {start_date} start -> df from first_index:{df.index[0]}>start_date: {start_date}"
                )
            elif missing_end:
                print(
                    f"some day is missing on candles between {start_date} end -> df last_index:{df.index[-1]}< end_date: {end_date}"
                )

            raise Exception(
                f"some day is missing on candles between {start_date} and {end_date}  -> df from {df.index[0]} and {df.index[-1]}"
            )

    def _try_regenerate_period_one(self,
                                   instrument_pk: str,
                                   start_date: datetime.datetime,
                                   end_date: datetime.datetime,
                                   resolution: str,
                                   num_units: int,
                                   first_hour=None,
                                   last_hour=None, ):

        try:
            unit_candles = self.get_candles_midprice_time(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                resolution=resolution,
                num_units=1,
                is_error_call=True,
                first_hour=first_hour,
                last_hour=last_hour,
            )
        except Exception as e:
            unit_candles = None

        if unit_candles is not None and len(unit_candles) > 0:
            resolution_used = resolution.lower()
            # group unit_candles by num_units take close price
            resampled = unit_candles[['close', 'low', 'high', 'open', 'date_time']].resample(
                f"{num_units}{resolution_used}").ohlc()
            agg = unit_candles[
                ['tick_num', 'volume', 'cum_buy_volume', 'cum_ticks', 'cum_dollar_value']].resample(
                f"{num_units}{resolution_used}").sum()
            last_rows = unit_candles[['date_time']].resample(
                f"{num_units}{resolution_used}").last()

            df = pd.DataFrame(index=resampled.index, columns=unit_candles.columns)
            df['date_time'] = resampled['date_time']['open']
            period_seconds = {'D': 86400, 'H': 3600, 'MIN': 60, 'S': 1}[resolution] * num_units
            df['openTimestamp'] = df['date_time']
            df['closeTimestamp'] = df['date_time'] + period_seconds

            df['open'] = resampled['open']['open']
            df['close'] = resampled['close']['close']
            df['high'] = resampled['high']['high']
            df['low'] = resampled['low']['low']

            df['tick_num'] = agg['tick_num']
            df['volume'] = agg['volume']
            df['cum_buy_volume'] = agg['cum_buy_volume']
            df['cum_ticks'] = agg['cum_ticks']
            df['cum_dollar_value'] = agg['cum_dollar_value']
            df.reset_index(inplace=True)
            df['datetime'] = pd.to_datetime(df['openTimestamp'] * 1E9)
            df = df.set_index('datetime')
            # drop duplicates index and keep first
            df = df[~df.index.duplicated(keep='first')].sort_index()
            del df['date']
            return df
        return None

    def _generate_from_depth(self,
                             instrument_pk: str,
                             start_date: datetime.datetime,
                             end_date: datetime.datetime,
                             resolution: str,
                             num_units: int,
                             first_hour=None,
                             last_hour=None
                             ):
        from database.candle_generation import generate_candle_time
        # add more time in boundaries
        depth_df = self.get_depth(
            instrument_pk=instrument_pk,
            start_date=start_date - BDay(2),
            end_date=end_date + BDay(2),
        )
        first_index = depth_df.index.min()
        last_index = depth_df.index.max()
        if first_index > start_date + datetime.timedelta(
                hours=8
        ) or last_index < end_date - datetime.timedelta(hours=8):
            print(
                rf"WARNING: something is wrong on {instrument_pk} to regenerate candles_midprice_time depth_df between first_index:{first_index}>start_date: {start_date}  or last_index:{last_index}<end_date: {end_date}"
            )

        if start_date == end_date:
            print(
                rf"WARNING _regenerate_candles_midprice_time: start_date==end_date={start_date} -> adding 24 hours to end_date and 0 to start_date")
            start_date = datetime.datetime.combine(start_date, datetime.datetime.min.time())
            end_date = datetime.datetime.combine(end_date, datetime.datetime.max.time())
        # get depth_df between start_time and end_time
        start_date_str = datetime.datetime.combine(start_date,
                                                   datetime.datetime.min.time())  # start_date.strftime("%Y-%m-%d")
        end_date_str = datetime.datetime.combine(end_date,
                                                 datetime.datetime.max.time())  # end_date.strftime("%Y-%m-%d")
        depth_df = depth_df[start_date_str:end_date_str]

        if resolution == 'D' and first_hour is not None and last_hour is not None:
            depth_df = depth_df.between_time(
                start_time=rf"{first_hour}:00", end_time=rf"{last_hour}:00"
            )
        # Get the list of column names
        cols = depth_df.columns.tolist()
        # Move the desired column to the first position
        cols.insert(0, cols.pop(cols.index('midprice')))
        # Reorder the DataFrame with the new column order
        depth_df = depth_df[cols]

        df = generate_candle_time(
            df=depth_df, resolution=resolution, num_units=num_units,
        )
        return df

    def _regenerate_candles_midprice_time(
            self,
            instrument_pk: str,
            start_date: datetime.datetime,
            end_date: datetime.datetime,
            resolution: str,
            num_units: int,
            source_path: str,
            first_hour=None,
            last_hour=None,
    ):

        df = self._try_regenerate_period_one(
            instrument_pk=instrument_pk,
            start_date=start_date,
            end_date=end_date,
            resolution=resolution,
            num_units=num_units,
            first_hour=first_hour,
            last_hour=last_hour,
        )

        if df is None:
            # Split into 3-month brackets
            candle_dfs = []
            current_start = start_date
            brackets_days_split = 90  # reduce if still found memory issues
            while current_start < end_date:
                # Calculate the end of current 3-month bracket
                current_end = min(current_start + datetime.timedelta(days=brackets_days_split), end_date)

                # Generate candles for this bracket
                df_bracket = self._generate_from_depth(
                    instrument_pk=instrument_pk,
                    start_date=current_start,
                    end_date=current_end,
                    resolution=resolution,
                    num_units=num_units,
                    first_hour=first_hour,
                    last_hour=last_hour,
                )

                if df_bracket is not None and len(df_bracket) > 0:
                    candle_dfs.append(df_bracket)

                # Move to next bracket
                current_start = current_end

            # Join all candle dataframes
            if not candle_dfs:
                raise Exception(f"No candles generated for {instrument_pk} between {start_date} and {end_date}")

            df = pd.concat(candle_dfs, axis=0)
            df = df[~df.index.duplicated(keep='first')].sort_index()
            candle_dfs = []

        df = df[~df.index.duplicated()].sort_index()
        self._persist_candles(
            df=df, start_date=start_date, end_date=end_date, source_path=source_path
        )

    @memory_cache(maxsize=512)
    def get_candles(self,
                    instrument_pk: str,
                    start_date: datetime.datetime = default_start_date,
                    end_date: datetime.datetime = default_end_date,
                    candle_type: CandleType = CandleType.CANDLE_MIDPRICE_TIME,
                    resolution: CandleTimeResolution = CandleTimeResolution.MIN,
                    num_units: int = 1,
                    is_error_call: bool = False,
                    first_hour=None,
                    last_hour=None, ) -> pd.DataFrame:
        '''

        Parameters
        ----------
        instrument_pk
        start_date
        end_date
        candle_type
        resolution
        num_units
        is_error_call
        first_hour
        last_hour

        Returns
        -------

        '''

        if candle_type == CandleType.CANDLE_MIDPRICE_TIME:
            # depth
            return self.get_candles_midprice_time(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                resolution=resolution,
                num_units=num_units,
                is_error_call=is_error_call,
                first_hour=first_hour,
                last_hour=last_hour,
            )
        elif candle_type == CandleType.CANDLE_TIME:
            # trades
            return self.get_candles_time(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                resolution=resolution,
                num_units=num_units,
                is_error_call=is_error_call,
                first_hour=first_hour,
                last_hour=last_hour,
            )
        elif candle_type == CandleType.CANDLE_TICK:
            # trades
            return self.get_candles_tick(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                number_of_ticks=num_units,
                is_error_call=is_error_call,
            )
        elif candle_type == CandleType.CANDLE_VOLUME:
            # trades
            return self.get_candles_volume(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                volume=num_units,
                is_error_call=is_error_call,
            )

    def _get_parquet_candles_by_date(self, source_path, start_date, end_date):
        import pyarrow.parquet as pq
        import pyarrow as pa
        start_date_str = start_date.strftime(
            self.date_str_format)  # minus 1 day to get the previous day restrictive greater
        end_date_str = end_date.strftime(self.date_str_format)

        # Because NAS has lower version of pyarrow
        if pa._generated_version.version_tuple[0] <= 7:
            # avoid error schema
            dataset = pq.ParquetDataset(
                source_path,
                filters=[
                    ('date', '>=', int(start_date_str)),
                    ('date', '<=', int(end_date_str)),
                ],
                validate_schema=False
            )
            all_columns = ['date_time', 'tick_num', 'open', 'high', 'low', 'close', 'volume',
                           'cum_buy_volume', 'cum_ticks', 'cum_dollar_value', 'date',
                           'openTimestamp', 'closeTimestamp', 'openTime', 'closeTime']
            all_columns = [column for column in all_columns if column in dataset.schema.names]
            table = dataset.read(columns=all_columns)
            df = table.to_pandas()
            df['date_time'] = df['date_time'].astype('int64')
            df['datetime'] = pd.to_datetime(df['date_time'] * 1000000000)
            df.set_index('datetime', inplace=True)
        else:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[
                    ('date', '>=', int(start_date_str)),
                    ('date', '<=', int(end_date_str)),
                ],
            )
            table = dataset.read()
            df = table.to_pandas()

        df = reduce_memory_usage(df)
        df.drop_duplicates(inplace=True)
        return df

    def get_candles_midprice_time(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            resolution: CandleTimeResolution = CandleTimeResolution.MIN,
            num_units: int = 1,
            is_error_call: bool = False,
            first_hour=None,
            last_hour=None,
    ):

        '''
        Get candles by time from depth -> price from midprice

        :param instrument_pk:
        :param start_date: included
        :param end_date: included
        :param resolution: (CandleTimeResolution) Resolution type ('D', 'H', 'MIN', 'S')
        :param num_units: (int) Number of resolution units (3 days for example, 2 hours)
        :param is_error_call:
        :return:
        '''

        from database.candle_generation import generate_candle_time
        e = ''
        start_date_str = start_date.strftime(
            self.date_str_format)  # minus 1 day to get the previous day restrictive greater
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_midpricetime_%s%d' % (resolution, num_units)
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        # source_path = os.path.normpath(source_path)
        if not os.path.isdir(source_path):
            print(f'creating candle_midpricetime {source_path} ...')
        df = None
        try:
            # print(
            #     "querying %s tick_db %s from %s to %s"
            #     % (type_data, instrument_pk, start_date_str, end_date_str)
            # )
            try:
                if os.path.isdir(source_path):
                    df = self._get_parquet_candles_by_date(source_path, start_date, end_date)

            except Exception as e:
                print(
                    f"WARNING: can't read using get_candles_midprice_time parquet {instrument_pk} {num_units}{resolution} between {start_date_str} and {end_date_str} {source_path} => try manual mode using pandas\n{e}"
                )
                df = self._get_manual_pandas(
                    instrument_pk=instrument_pk,
                    start_date=start_date,
                    end_date=end_date,
                    type_data=type_data,
                )
                df = reduce_memory_usage(df)
                df.drop_duplicates(inplace=True)

            if df is None or len(df) == 0:
                raise Exception(
                    rf"no data get_candles_midprice_time found {instrument_pk} {num_units}{resolution} between {start_date_str} and {end_date_str}"
                )

            self._check_all_candles_exist(
                df=df, start_date=start_date, end_date=end_date
            )

        except Exception as e:
            if is_error_call:
                raise e

            if df is None or len(df) == 0:
                regenerate_start_date = datetime.datetime.combine(start_date, datetime.datetime.min.time())
                regenerate_end_date = datetime.datetime.combine(end_date, datetime.datetime.max.time())
            else:
                missing_start = df.index[0] - BDay(2) > start_date
                if missing_start:
                    regenerate_start_date = datetime.datetime.combine(start_date, datetime.datetime.min.time())
                else:
                    print(f"not missing start date {start_date} ->regenerate_start_date =  {df.index[-1]}")
                    regenerate_start_date = df.index[-1]

                missing_end = df.index[-1] + BDay(1) < end_date
                if missing_end:
                    regenerate_end_date = datetime.datetime.combine(end_date, datetime.datetime.max.time())
                else:
                    print(f"not missing end date {end_date} ->regenerate_end_date =  {df.index[0]}")
                    regenerate_end_date = df.index[0]

            print(
                rf"WARNING: Exception get_candles_midprice_time {e} -> _regenerate_candles_midprice_time {num_units}{resolution} between {regenerate_start_date} and {regenerate_end_date}")
            self._regenerate_candles_midprice_time(
                instrument_pk=instrument_pk,
                start_date=regenerate_start_date,
                end_date=regenerate_end_date,
                resolution=resolution,
                num_units=num_units,
                source_path=source_path,
                first_hour=first_hour,
                last_hour=last_hour,
            )
            df = self.get_candles_midprice_time(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                resolution=resolution,
                num_units=num_units,
                is_error_call=True,
            )

            # self._check_all_candles_exist(
            #     df=df, start_date=start_date, end_date=end_date
            # )

        if resolution != 'D' and first_hour is not None and last_hour is not None:
            df = df.between_time(
                start_time=rf"{first_hour}:00", end_time=rf"{last_hour}:00"
            )
        if start_date == end_date:
            print(
                rf"WARNING: get_candles_midprice_time: start_date==end_date={start_date} -> adding 24 hours to end_date and 0 to start_date")
            start_date = datetime.datetime.combine(start_date, datetime.datetime.min.time())
            end_date = datetime.datetime.combine(end_date, datetime.datetime.max.time())

        df.index = pd.to_datetime(df.index)
        df.sort_index(inplace=True)
        df = df[start_date:end_date]

        return self._ensure_candle_times(
            df, {'D': 86400, 'H': 3600, 'MIN': 60, 'S': 1}[resolution] * num_units
        )

    def _get_manual_pandas(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            type_data: str = 'depth',
            filename: str = 'data.parquet',
    ):

        # type_data = 'depth'
        source_path = os.path.join(
            self.base_path, f"type={type_data}", f"instrument={instrument_pk}"
        )
        # X:\lambda_data\type=depth\instrument=eurusd_darwinex\date=20211112
        # filename = "data.parquet"
        current_date = start_date
        output = None
        try:
            while current_date < end_date:
                current_date_str = current_date.strftime(self.date_str_format)
                filename_complete = os.path.join(
                    source_path, f"date={current_date_str}", f"{filename}"
                )
                if os.path.exists(filename_complete):
                    try:
                        df_temp = pd.read_parquet(filename_complete)
                        if output is None:
                            output = df_temp
                        else:
                            output = pd.concat([output, df_temp])
                    except Exception as e1:
                        print(f"ERROR _get_manual_pandas 1 {current_date_str} {filename} {filename_complete} {e1}")
                        raise e1

                current_date += datetime.timedelta(days=1)
        except Exception as e:
            print(f"ERROR _get_manual_pandas 2 {e}")
            raise e
        return output

    def get_candles_time(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            resolution: CandleTimeResolution = CandleTimeResolution.MIN,
            num_units: int = 1,
            is_error_call: bool = False,
            first_hour=None,
            last_hour=None,
    ):
        import pyarrow.parquet as pq

        '''
        Get candles by time from trades -> price from trades
        :param instrument_pk:
        :param start_date: included
        :param end_date: included
        :param resolution: (CandleTimeResolution) Resolution type ('D', 'H', 'MIN', 'S')
        :param num_units: (int) Number of resolution units (3 days for example, 2 hours)
        :param is_error_call:
        :return:
        '''
        start_date, end_date = self._check_start_date_end_date(start_date, end_date)

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_time_%s%d' % (resolution, num_units)
        source_path = os.path.join(
            self.base_path, f"type={type_data}", f"instrument={instrument_pk}"
        )

        if not os.path.isdir(source_path):
            print(f'creating candle_time {source_path} ...')

        try:
            # print(
            #     "querying %s tick_db %s from %s to %s"
            #     % (type_data, instrument_pk, start_date_str, end_date_str)
            # )

            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', int(start_date_str)), ('date', '<=', int(end_date_str))],
            )
            table = dataset.read()
            df = table.to_pandas()
            df.drop_duplicates(inplace=True)
            self._check_all_candles_exist(
                df=df, start_date=start_date, end_date=end_date
            )

        except Exception as e:
            if is_error_call:
                raise e
            trades_df = self.get_trades(
                instrument_pk=instrument_pk, start_date=start_date, end_date=end_date
            )
            from database.candle_generation import generate_candle_time_legacy

            if resolution == 'D' and first_hour is not None and last_hour is not None:
                trades_df = trades_df.between_time(
                    start_time=rf"{first_hour}:00", end_time=rf"{last_hour}:00"
                )

            df = generate_candle_time_legacy(
                df=trades_df, resolution=resolution, num_units=num_units
            )
            df = df[~df.index.duplicated()]

            self._persist_candles(
                df=df, start_date=start_date, end_date=end_date, source_path=source_path
            )
            df = self.get_candles_time(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                resolution=resolution,
                num_units=num_units,
                is_error_call=True,
            )

        if resolution != 'D' and first_hour is not None and last_hour is not None:
            df = df.between_time(
                start_time=rf"{first_hour}:00", end_time=rf"{last_hour}:00"
            )
        df.index = pd.to_datetime(df.index)
        df.sort_index(inplace=True)
        return self._ensure_candle_times(
            df, {'D': 86400, 'H': 3600, 'MIN': 60, 'S': 1}[resolution] * num_units
        )

    def get_candles_tick(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            number_of_ticks: int = 100,
            is_error_call: bool = False,
    ):
        '''
        Get candles by tick from trades -> price from trades
        Parameters
        ----------
        instrument_pk
        start_date : included
        end_date : included
        number_of_ticks
        is_error_call

        Returns
        -------

        '''
        import pyarrow as pq

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_tick_%d' % (number_of_ticks)
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        # source_path = os.path.normpath(source_path)
        try:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', int(start_date_str)), ('date', '<=', int(end_date_str))],
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
            from database.candle_generation import generate_candle_tick

            df = generate_candle_tick(df=trades_df, number_of_ticks=number_of_ticks)
            self._persist_candles(
                df=df, start_date=start_date, end_date=end_date, source_path=source_path
            )
            return self.get_candles_tick(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                number_of_ticks=number_of_ticks,
                is_error_call=True,
            )
        df = df[start_date:end_date]
        return self._ensure_candle_times(df)

    def get_candles_volume(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            volume: float = 100,
            is_error_call: bool = False,
    ):
        '''
        Get candles by volume from trades -> price from trades

        Parameters
        ----------
        instrument_pk
        start_date : included
        end_date : included
        volume
        is_error_call

        Returns
        -------

        '''
        import pyarrow.parquet as pq

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_volume_%f' % (volume)
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        try:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', int(start_date_str)), ('date', '<=', int(end_date_str))],
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
            from database.candle_generation import generate_candle_volume

            df = generate_candle_volume(df=trades_df, volume=volume)
            self._persist_candles(
                df=df, start_date=start_date, end_date=end_date, source_path=source_path
            )
            return self.get_candles_volume(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                volume=volume,
                is_error_call=True,
            )
        df = df[start_date:end_date]
        return self._ensure_candle_times(df)

    def get_candles_dollar_value(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            dollar_value: float = 1000,
            is_error_call: bool = False,
    ):
        '''

        Parameters
        ----------
        instrument_pk
        start_date : included
        end_date : included
        dollar_value
        is_error_call

        Returns
        -------

        '''
        import pyarrow.parquet as pq

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_dollar_value_%d' % (dollar_value)
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        try:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', int(start_date_str)), ('date', '<=', int(end_date_str))],
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
            from database.candle_generation import generate_candle_dollar_value

            df = generate_candle_dollar_value(df=trades_df, dollar_value=dollar_value)
            self._persist_candles(
                df=df, start_date=start_date, end_date=end_date, source_path=source_path
            )
            return self.get_candles_dollar_value(
                instrument_pk=instrument_pk,
                start_date=start_date,
                end_date=end_date,
                dollar_value=dollar_value,
                is_error_call=True,
            )
        df = df[start_date:end_date]
        return self._ensure_candle_times(df)


if __name__ == '__main__':
    tick = TickDB()
    instrument_pk = 'btceur_binance'
    from utils.pandas_utils.dataframe_utils import garman_klass_volatility

    # instruments = tick.get_all_instruments()
    dates = tick.get_all_dates(type_str='depth', instrument_pk=instrument_pk)
    # trades_df_all = tick.get_all_trades(instrument_pk=LambdaInstrument.btcusdt_binance)
    start_date = datetime.datetime(year=2022, day=6, month=6)
    end_date = datetime.datetime(year=2022, day=13, month=6)
    candles_df = tick.get_candles_midprice_time(
        instrument_pk=instrument_pk,
        start_date=start_date,
        end_date=end_date,
        first_hour=7,
        last_hour=15,
        resolution='D',
    )

    high_series = candles_df['high']
    low_series = candles_df['low']
    open_series = candles_df['open']
    close_series = candles_df['close']

    sigma_gk = garman_klass_volatility(
        high=high_series,
        low=low_series,
        open=open_series,
        close=close_series,
        trading_periods=365,
    )
    sigma_std = close_series.std() * (365 ** 0.5)
    print(sigma_gk)
    # trades_df = tick.get_trades(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date, first_hour=7,
    #                             last_hour=15)
    # depth_df = tick.get_depth(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date, first_hour=7,
    #                           last_hour=15)
    # all = tick.get_all_data(instrument_pk=instrument_pk, start_date=start_date, end_date=end_date, first_hour=7,
    #                         last_hour=15)

    # candle_time = tick.get_candles_time(instrument_pk=instrument_pk,
    #                                     start_date=datetime.datetime(year=2020, day=7, month=12),
    #                                     end_date=datetime.datetime(year=2020, day=7, month=12))
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

    # candle_dollar_volume = tick.get_candles_dollar_value(instrument_pk=instrument_pk,
    #                                                      start_date=datetime.datetime(year=2020, day=7, month=12),
    #                                                      end_date=datetime.datetime(year=2020, day=7, month=12),
    #                                                      dollar_value=1000
    #                                                      )
