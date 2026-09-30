import unittest
from database.tick_db import TickDB, CandleTimeResolution
import datetime

from test.utils.test_util import set_test_data_path


class TestCandlesTickDb(unittest.TestCase):
    # set env variables LAMBDA_DATA_PATH to current test folder in data ddbb
    data_path = set_test_data_path()
    tick = TickDB(root_db_path=data_path)

    start_date = datetime.datetime(day=14, month=3, year=2024)
    end_date = datetime.datetime(day=14, month=3, year=2024)
    instrument_test = 'eurgbp_darwinex'

    num_units_generated = 5
    resolution_generated = CandleTimeResolution.MIN

    def clean_previous_candles(self):
        import os
        import shutil
        candles_path1 = os.path.join(self.data_path,
                                     rf'type=candle_midpricetime_{TestCandlesTickDb.resolution_generated}1/instrument={TestCandlesTickDb.instrument_test}')
        candles_path2 = os.path.join(self.data_path,
                                     rf'type=candle_midpricetime_{TestCandlesTickDb.resolution_generated}{TestCandlesTickDb.num_units_generated}/instrument={TestCandlesTickDb.instrument_test}')
        check_paths = [candles_path1, candles_path2]
        for path in check_paths:
            shutil.rmtree(path, ignore_errors=True)

    def test_get_candles_generate(self):
        self.clean_previous_candles()
        candles_midprice_5 = self.tick.get_candles_midprice_time(
            instrument_pk=self.instrument_test,
            start_date=self.start_date,
            end_date=self.end_date,
            resolution=self.resolution_generated,
            num_units=self.num_units_generated,
        )
        self.assertIsNotNone(candles_midprice_5)
        self.assertIn('openTime', candles_midprice_5.columns)
        self.assertIn('closeTime', candles_midprice_5.columns)
        self.assertEqual(
            candles_midprice_5['closeTime'].iloc[0] - candles_midprice_5['openTime'].iloc[0],
            TestCandlesTickDb.num_units_generated * 60,
        )

        candles_midprice_1 = self.tick.get_candles_midprice_time(
            instrument_pk=self.instrument_test,
            start_date=self.start_date,
            end_date=self.end_date,
            resolution=self.resolution_generated,
            num_units=1,
        )
        self.assertIsNotNone(candles_midprice_1)
        self.assertIn('openTime', candles_midprice_1.columns)
        self.assertIn('closeTime', candles_midprice_1.columns)

        num_unit_generated = self.tick._try_regenerate_period_one(
            instrument_pk=self.instrument_test,
            start_date=self.start_date,
            end_date=self.end_date,
            resolution=self.resolution_generated,
            num_units=self.num_units_generated
        )
        self.assertIsNotNone(num_unit_generated)

        self.assertEqual(len(num_unit_generated), len(candles_midprice_5))
        self.assertEqual(len(num_unit_generated), len(candles_midprice_1) // 5)

        sum_diff = (candles_midprice_1['close'] - num_unit_generated['close']).sum()
        self.assertTrue(sum_diff == 0)
