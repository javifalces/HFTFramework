from backtest.rsi_dqn import RsiDQN
import datetime

# %%
parameters = {'style': 'aggressive',
              'seed': 2,
              'maxBatchSize': 5000,
              'trainingPredictIterationPeriod': -1,
              'trainingTargetIterationPeriod': -1,
              'epoch': 50,
              'scoreEnum': 'total_pnl',
              'epsilon': 0.2,
              'discountFactor': 0.95,
              'learningRate': 0.95,
              'quantity': 0.001,
              'first_hour': 0,
              'last_hour': 24,
              'l1': 0.0,
              'l2': 0.0,
              'levelToQuote': 1,
              'periods': [3.0,6.0,9.0,13.0],
              # 'upperBounds': [66.6323472170498, 60.0],
              # 'upperBoundsExits': [45.0, 46.7601387000739],
              # 'lowerBounds': [25.0, 38.61473802817327],
              # 'lowerBoundsExits': [52.92249097682304, 45.0],
              "changeSides": [0],
              'stateColumnsFilter': [],
              "useAsQLearn": 1
              }

instrument_pk = 'eurusd_darwinex'
algorithm_info = '%s_test_script' % instrument_pk
rsi_dqn = RsiDQN(algorithm_info=algorithm_info)
rsi_dqn.set_parameters(parameters=parameters)
# %%
output_train = rsi_dqn.train(
    instrument_pk=instrument_pk,
    start_date=datetime.datetime(year=2021, day=4, month=6, hour=0),
    end_date=datetime.datetime(year=2021, day=4, month=6, hour=14),
    iterations=-1,
    algos_per_iteration=2,
    simultaneous_algos=2,
    patience=15,
    clean_initial_experience=True,
    # force_explore_prob=0.0
)
# %%
import matplotlib.pyplot as plt
import os

legend_dict = []
counter = 0
plt.close()
plt.ion()
for iteration in output_train:
    legend_dict.append(f'iteration_{counter}')
    iteration[rsi_dqn.NAME + '_' + algorithm_info]['historicalRealizedPnl'].plot()
    counter += 1
    import time;

    time.sleep(3)
plt.legend(legend_dict)
# %%
import pandas as pd

plt.close()
final_pnl = []
for iteration in output_train:
    final_pnl.append(iteration[rsi_dqn.NAME + '_' + algorithm_info]['historicalRealizedPnl'].iloc[-1])
final_pnl_serie = pd.Series(final_pnl)
final_pnl_serie.plot()
plt.xlabel("iteration")
plt.ylabel("final realized pnl")
# %%
