import datetime
from pathlib import Path

import pandas as pd

import os
from glob import glob

from pandas._libs.tslibs.offsets import BDay

from configuration import LAMBDA_DATA_PATH


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

    def get_all_instruments(self, type_str: str = 'trade') -> list:
        source_path = rf"{self.base_path}/type={type_str}"
        # source_path = os.path.normpath(source_path)
        all_folders = glob(source_path + "/*")
        instruments = []
        for folder in all_folders:
            instrument = folder.split("instrument=")[-1]
            instruments.append(instrument)
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

    def get_depth(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            first_hour: int = None,
            last_hour: int = None,
            columns: list = None,
    ):

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'depth'
        source_path = rf"{self.base_path}\type={type_data}\instrument={instrument_pk}"

        # source_path = os.path.normpath(source_path)
        if not os.path.isdir(source_path):
            print(f'creating DEPTH {source_path} ...')

        print(
            "querying %s tick_db %s from %s to %s"
            % (type_data, instrument_pk, start_date_str, end_date_str)
        )
        try:
            import pyarrow.parquet as pq

            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<', end_date_str)],
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
        if df is None:
            print(
                rf"WARNING: no data found {instrument_pk} between {start_date_str} and {end_date_str} -> None"
            )
            return None
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
            df = df[start_date:]
        if end_date is not None:
            df = df[:end_date]
        return df

    def create_date_index(self, df) -> pd.DataFrame:
        if df.index[0] == 0 and df.index[1] == 1 and 'timestamp' in df.columns:
            df.set_index('timestamp', inplace=True)
        df['date'] = pd.to_datetime(df.index * 1000000)
        return df

    def get_trades(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            first_hour: int = None,
            last_hour: int = None,
    ):
        import pyarrow.parquet as pq

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'trade'
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        if not os.path.isdir(source_path):
            print(f'creating TRADE {source_path} ...')

        print(
            "querying %s tick_db %s from %s to %s"
            % (type_data, instrument_pk, start_date_str, end_date_str)
        )
        dataset = pq.ParquetDataset(
            source_path,
            filters=[('date', '>=', start_date_str), ('date', '<', end_date_str)],
        )
        table = dataset.read()
        df = table.to_pandas()
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
        return df

    def _persist_candles(
            self,
            source_path: str,
            df: pd.DataFrame,
            start_date: datetime.datetime,
            end_date: datetime.datetime,
    ):

        # ignore warnings
        import warnings
        from pandas.core.common import SettingWithCopyWarning

        warnings.filterwarnings(category=SettingWithCopyWarning, action="ignore")

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
                    df_to_persist.to_parquet(
                        file_path, compression='GZIP', engine='fastparquet'
                    )
                except Exception as e:
                    df_to_persist.to_parquet(file_path, compression='GZIP')
                # table = pa.Table.from_pandas(df_to_persist)
                # pq.write_table(table, complete_path + os.sep + 'data.parquet',compression='GZIP')

            day_to_persist = next_day

    def _check_all_candles_exist(
            self,
            df: pd.DataFrame,
            start_date: datetime.datetime,
            end_date: datetime.datetime,
    ):

        if df.index[0] - BDay(2) > start_date or df.index[-1] + BDay(1) < end_date:
            print(
                f"some day is missing on candles between {start_date} and {end_date}  -> df from first_index:{df.index[0]}>start_date: {start_date} or last_index:{df.index[-1]}< end_date: {end_date}"
            )
            raise Exception(
                f"some day is missing on candles between {start_date} and {end_date}  -> df from {df.index[0]} and {df.index[-1]}"
            )

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
        depth_df = depth_df[start_date:end_date]
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
            df=depth_df, resolution=resolution, num_units=num_units
        )
        df = df[~df.index.duplicated()]

        self._persist_candles(
            df=df, start_date=start_date, end_date=end_date, source_path=source_path
        )

    def get_candles_midprice_time(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            resolution: str = 'MIN',
            num_units: int = 1,
            is_error_call: bool = False,
            first_hour=None,
            last_hour=None,
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

        from database.candle_generation import generate_candle_time

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_midpricetime_%s%d' % (resolution, num_units)
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        # source_path = os.path.normpath(source_path)
        if not os.path.isdir(source_path):
            print(f'creating candle_midpricetime {source_path} ...')
        try:
            print(
                "querying %s tick_db %s from %s to %s"
                % (type_data, instrument_pk, start_date_str, end_date_str)
            )
            try:
                import pyarrow.parquet as pq

                df = None
                if os.path.isdir(source_path):
                    dataset = pq.ParquetDataset(
                        source_path,
                        filters=[
                            ('date', '>=', start_date_str),
                            ('date', '<', end_date_str),
                        ],
                    )
                    table = dataset.read()
                    df = table.to_pandas()
                    df.drop_duplicates(inplace=True)

            except Exception as e:
                print(
                    f"cant read using get_candles_midprice_time parquet {instrument_pk} between {start_date_str} and {end_date_str} {source_path} => try manual mode using pandas\n{e}"
                )
                df = self._get_manual_pandas(
                    instrument_pk=instrument_pk,
                    start_date=start_date,
                    end_date=end_date,
                    type_data=type_data,
                )
                df.drop_duplicates(inplace=True)

            if df is None:
                print(
                    rf"WARNING: no data get_candles_midprice_time found {instrument_pk} between {start_date_str} and {end_date_str} -> None"
                )

            self._check_all_candles_exist(
                df=df, start_date=start_date, end_date=end_date
            )

        except Exception as e:
            if is_error_call:
                raise e
            self._regenerate_candles_midprice_time(
                instrument_pk,
                start_date,
                end_date,
                resolution,
                num_units,
                source_path,
                first_hour,
                last_hour,
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

        df = df[start_date:end_date]
        return df

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
        while current_date < end_date:
            current_date_str = current_date.strftime(self.date_str_format)
            filename_complete = os.path.join(
                source_path, f"date={current_date_str}", f"{filename}"
            )
            if os.path.exists(filename_complete):
                df_temp = pd.read_parquet(filename_complete)
                if output is None:
                    output = df_temp
                else:
                    output = pd.concat([output, df_temp])
            current_date += datetime.timedelta(days=1)
        return output

    def get_candles_time(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            resolution: str = 'MIN',
            num_units: int = 1,
            is_error_call: bool = False,
            first_hour=None,
            last_hour=None,
    ):
        import pyarrow.parquet as pq

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
        source_path = os.path.join(
            self.base_path, f"type={type_data}", f"instrument={instrument_pk}"
        )

        if not os.path.isdir(source_path):
            print(f'creating candle_time {source_path} ...')

        try:
            print(
                "querying %s tick_db %s from %s to %s"
                % (type_data, instrument_pk, start_date_str, end_date_str)
            )

            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<', end_date_str)],
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
            from database.candle_generation import generate_candle_time

            if resolution == 'D' and first_hour is not None and last_hour is not None:
                trades_df = trades_df.between_time(
                    start_time=rf"{first_hour}:00", end_time=rf"{last_hour}:00"
                )

            df = generate_candle_time(
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

        return df

    def get_candles_tick(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            number_of_ticks: int = 100,
            is_error_call: bool = False,
    ):
        import pyarrow as pq

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_tick_%d' % (number_of_ticks)
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        # source_path = os.path.normpath(source_path)
        try:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<', end_date_str)],
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
        return df

    def get_candles_volume(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            volume: float = 100,
            is_error_call: bool = False,
    ):
        import pyarrow.parquet as pq

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_volume_%f' % (volume)
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        try:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<', end_date_str)],
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
        return df

    def get_candles_dollar_value(
            self,
            instrument_pk: str,
            start_date: datetime.datetime = default_start_date,
            end_date: datetime.datetime = default_end_date,
            dollar_value: float = 1000,
            is_error_call: bool = False,
    ):
        import pyarrow.parquet as pq

        start_date_str = start_date.strftime(self.date_str_format)
        end_date_str = end_date.strftime(self.date_str_format)
        type_data = 'candle_dollar_value_%d' % (dollar_value)
        source_path = rf"{self.base_path}/type={type_data}/instrument={instrument_pk}"

        try:
            dataset = pq.ParquetDataset(
                source_path,
                filters=[('date', '>=', start_date_str), ('date', '<', end_date_str)],
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
        return df


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
