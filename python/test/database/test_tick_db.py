import unittest
from database.tick_db import TickDB
import datetime


class TestTickDb(unittest.TestCase):
    tick = TickDB()
    start_date = datetime.datetime(day=6, month=9, year=2021)
    end_date = datetime.datetime(day=16, month=9, year=2021)
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

    def test_get_depth_fastparquet(self):
        import pandas as pd

        depth_df = self.tick.get_depth(
            instrument_pk=self.instrument_test,
            start_date=self.start_date,
            end_date=self.end_date,
        )

        depth_df2 = self.tick._get_depth_pandas(
            instrument_pk=self.instrument_test,
            start_date=self.start_date,
            end_date=self.end_date,
        )
        depth_df2['date'] = pd.to_datetime(depth_df2.index * 1000000)
        depth_df2.set_index('date', inplace=True)
        # add basic indicators
        depth_df2['midprice'] = (depth_df2['askPrice0'] + depth_df2['bidPrice0']) / 2
        depth_df2['spread'] = (depth_df2['askPrice0'] - depth_df2['bidPrice0']).abs()

        self.assertIsNotNone(depth_df)
        self.assertIsNotNone(depth_df2)

        self.assertEquals(depth_df.shape[0], depth_df2.shape[0])
        self.assertEquals(depth_df.shape[1], depth_df2.shape[1])
