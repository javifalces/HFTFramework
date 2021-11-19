#%%
from backtest.avellaneda_stoikov import AvellanedaStoikov
from backtest.backtest_launcher import REMOVE_INPUT_JSON
import datetime
#%%

avellaneda_stoikov_1 = AvellanedaStoikov(algorithm_info='test_main_1')
parameters_1={
    "risk_aversion": 0.9,
    "window_tick":3,
    "spread_multiplier":1.
}
avellaneda_stoikov_1.set_parameters(parameters_1)

avellaneda_stoikov_2 = AvellanedaStoikov(algorithm_info='test_main_2')
parameters_2={
    "risk_aversion": 0.1,
    "window_tick":33,
    "spread_multiplier":4.
}
avellaneda_stoikov_2.set_parameters(parameters_2)
#%%
# REMOVE_INPUT_JSON=False
instrument_pk='btcusdt_binance'
start_date = datetime.datetime(year=2020, day=9, month=12, hour=10)
end_date=datetime.datetime(year=2020, day=9, month=12,hour=19)

output_test_1 = avellaneda_stoikov_1.test(
        instrument_pk=instrument_pk,
        start_date=start_date,
        end_date=end_date,
    )

output_test_2 = avellaneda_stoikov_2.test(
        instrument_pk=instrument_pk,
        start_date=start_date,
        end_date=end_date,
    )
#%%