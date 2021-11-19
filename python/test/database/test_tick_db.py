import unittest
from database.tick_db import TickDB
import datetime

class TestTickDb(unittest.TestCase):
    tick = TickDB()
    start_date = datetime.datetime(day=6, month=9, year=2021)
    end_date = datetime.datetime(day=16, month=9, year=2021)
    instrument_test = 'eurusd_darwinex'

    def test_get_depth(self):
        depth_df = self.tick.get_depth(instrument_pk=self.instrument_test,
                                  start_date=self.start_date,
                                  end_date=self.end_date)
        self.assertIsNotNone(depth_df)

    def test_get_candles_midprice(self):
        candles_midprice = self.tick.get_candles_midprice_time(instrument_pk=self.instrument_test,
                                  start_date=self.start_date,
                                  end_date=self.end_date, resolution='MIN',
                                     num_units=1)
        self.assertIsNotNone(candles_midprice)