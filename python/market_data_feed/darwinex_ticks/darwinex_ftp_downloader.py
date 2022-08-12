# %%
import darwinex_ticks
import pandas as pd
import datetime
import os
from pathlib import Path
import copy

from configuration import PARQUET_PATH_DB


class DarwinexFtpDownloader:
    def __init__(
        self,
        username: str,
        password: str,
        host: str = 'tickdata.darwinex.com',
        port: int = 21,
        parquet_base_path: str = None,
    ):
        self.username = username
        self.password = password
        self.host = host
        self.port = port
        if parquet_base_path is None:
            parquet_base_path = PARQUET_PATH_DB
        self.parquet_base_path = parquet_base_path
        self.dwt = darwinex_ticks.DarwinexTicksConnection(
            dwx_ftp_user=self.username,
            dwx_ftp_pass=self.password,
            dwx_ftp_hostname=self.host,
            dwx_ftp_port=self.port,
        )
        # "%m/%d/%Y, %H:%M:%S"
        self.darwinex_date_format = '%Y-%m-%d %H'
        self.parquet_date_format = '%Y%m%d'  # 20210310

    def _download_ftp_data(
        self, symbol: str, start_date: datetime.datetime, end_date: datetime.datetime
    ) -> pd.DataFrame:
        '''

        :param symbol:
        :param start_date: included
        :param end_date: included
        :return: dataframe with columns Ask,Ask_size,Bid,Bid_size
        '''
        # '2018-08-02 08'
        #
        start = start_date.strftime(self.darwinex_date_format)
        end = end_date.strftime(self.darwinex_date_format)
        return self.dwt.ticks_from_darwinex(symbol.upper(), start=start, end=end)

    def download_depth(
        self, symbol: str, start_date: datetime.datetime, end_date: datetime.datetime
    ) -> pd.DataFrame:
        tick_data = self._download_ftp_data(
            symbol=symbol, start_date=start_date, end_date=end_date
        )
        # askPrice0 bidPrice0 askQuantity0 bidQuantity0 index is a timestamp
        column_rename_dict = {
            'Ask': 'askPrice0',
            'Bid': 'bidPrice0',
            'Ask_size': 'askQuantity0',
            'Bid_size': 'bidQuantity0',
        }
        rest_of_columns = ['askPrice', 'bidPrice', 'askQuantity', 'bidQuantity']
        rest_of_levels = [1, 2, 3, 4]
        tick_data.rename(columns=column_rename_dict, inplace=True)
        tick_data.reset_index(inplace=True)
        tick_data['timestamp'] = tick_data['Time'].astype('int64') // 10**6
        del tick_data['Time']
        tick_data.set_index('timestamp', inplace=True)
        for column in rest_of_columns:
            for level in rest_of_levels:
                tick_data['%s%d' % (column, level)] = None
        return tick_data

    def _save_parquet(self, symbol, day_to_add, df):
        typeData = 'depth'
        instrument_pk = '%s_darwinex' % symbol.lower()
        date_str = day_to_add.strftime(self.parquet_date_format)

        output_path = (
            self.parquet_base_path
            + os.sep
            + 'type=%s' % typeData
            + os.sep
            + 'instrument=%s' % instrument_pk
            + os.sep
            + 'date=%s' % date_str
            + os.sep
        )
        Path(output_path).mkdir(parents=True, exist_ok=True)
        try:
            df.to_parquet(
                output_path + 'data.parquet', compression='GZIP', engine='fastparquet'
            )
        except:
            df.to_parquet(output_path + 'data.parquet', compression='GZIP')

        print(f'{symbol}-{day_to_add} of {len(df)} rows saved to {output_path}')

    def update_parquet_db(
        self, symbol: str, start_date: datetime.datetime, end_date: datetime.datetime
    ):
        '''

        :param symbol: EURUSD or GBPUSD
        :param start_date: day included
        :param end_date: day included
        :return:
        '''
        day_to_add = start_date
        while day_to_add <= end_date:
            if day_to_add.weekday() == 5:
                # saturdays are skipped
                day_to_add = day_to_add + datetime.timedelta(days=1)
                continue

            start_date_temp = copy.copy(day_to_add)
            start_date_temp = start_date_temp.replace(hour=0)

            end_date_temp = copy.copy(day_to_add)
            end_date_temp = end_date_temp.replace(hour=23)
            try:
                df_to_persist = self.download_depth(
                    symbol=symbol, start_date=start_date_temp, end_date=end_date_temp
                )
                self._save_parquet(
                    symbol=symbol, day_to_add=day_to_add, df=df_to_persist
                )
            except Exception as e:
                print(f'some error downloading for {symbol} to day {day_to_add} -> {e}')

            day_to_add = day_to_add + datetime.timedelta(days=1)


# %%
if __name__ == '__main__':
    darwinex_ftp_downloader = DarwinexFtpDownloader(
        username='xxxx', password='xxxxx'
    )
    tick_data_darwinex = darwinex_ftp_downloader.update_parquet_db(
        symbol='EURUSD',
        start_date=datetime.datetime(year=2021, month=6, day=1),
        end_date=datetime.datetime(year=2021, month=6, day=8),
    )
