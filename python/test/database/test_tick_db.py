import unittest
from database.tick_db import TickDB
import datetime

from test.utils.test_util import set_test_data_path


class TestTickDb(unittest.TestCase):
    # set env variables LAMBDA_DATA_PATH to current test folder in data ddbb
    data_path = set_test_data_path()
    tick = TickDB(root_db_path=data_path)

    start_date = datetime.datetime(day=6, month=9, year=2021)
    end_date = datetime.datetime(day=9, month=9, year=2021)
    instrument_test = 'eurusd_darwinex'

    def test_get_depth(self):
        depth_df = self.tick.get_depth(
            instrument_pk=self.instrument_test,
            start_date=self.start_date,
            end_date=self.end_date,
        )
        self.assertIsNotNone(depth_df)

    def test_get_candles_midprice(self):
        candles_midprice = self.tick.get_candles_midprice_time(
            instrument_pk=self.instrument_test,
            start_date=self.start_date,
            end_date=self.end_date,
            resolution='MIN',
            num_units=1,
        )
        self.assertIsNotNone(candles_midprice)

    @unittest.skip("slow not needed")
    def test_get_depth_fastparquet(self):
        import pandas as pd

        start_date = self.start_date
        end_date = self.end_date
        depth_df = self.tick.get_depth(
            instrument_pk=self.instrument_test,
            start_date=start_date,
            end_date=end_date,
        )

        depth_df2 = self.tick._get_manual_pandas(
            instrument_pk=self.instrument_test,
            start_date=start_date,
            end_date=end_date,
        )
        depth_df2['date'] = pd.to_datetime(depth_df2.index * 1000000)
        depth_df2.set_index('date', inplace=True)
        # add basic indicators
        depth_df2['midprice'] = (depth_df2['askPrice0'] + depth_df2['bidPrice0']) / 2
        depth_df2['spread'] = (depth_df2['askPrice0'] - depth_df2['bidPrice0']).abs()

        self.assertIsNotNone(depth_df)
        self.assertIsNotNone(depth_df2)

        self.assertEqual(depth_df.shape[0], depth_df2.shape[0])
        self.assertEqual(depth_df.shape[1], depth_df2.shape[1])

    @unittest.skip("slow not needed")
    def test_get_candles_fastparquet(self):
        import pandas as pd

        instrument_test = 'eurusd_darwinex'
        start_date = self.start_date
        end_date = self.end_date
        candle_df = self.tick.get_candles_midprice_time(
            instrument_pk=instrument_test,
            start_date=start_date,
            end_date=end_date,
        )

        candle_df2 = self.tick._get_manual_pandas(
            instrument_pk=instrument_test,
            start_date=start_date,
            end_date=end_date,
            type_data='candle_midpricetime_MIN1',
        )
        candle_df2.drop_duplicates(inplace=True)
        candle_df2['date'] = pd.to_datetime(candle_df2.index)

        self.assertIsNotNone(candle_df)
        self.assertIsNotNone(candle_df2)

        self.assertEqual(candle_df.shape[0], candle_df2.shape[0])
        self.assertEqual(candle_df.shape[1], candle_df2.shape[1])
