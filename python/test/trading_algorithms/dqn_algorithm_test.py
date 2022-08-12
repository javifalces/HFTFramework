from trading_algorithms.side_quoting.rsi_dqn import RsiDQN
import numpy as np


class DqnAlgorithmTest:
    # %%
    parameters = {
        'style': 'aggressive',
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
        'periods': [3.0, 6.0, 9.0, 13.0],
        # 'upperBounds': [66.6323472170498, 60.0],
        # 'upperBoundsExits': [45.0, 46.7601387000739],
        # 'lowerBounds': [25.0, 38.61473802817327],
        # 'lowerBoundsExits': [52.92249097682304, 45.0],
        "changeSides": [0],
        'stateColumnsFilter': [],
        "useAsQLearn": 1,
    }

    instrument_pk = 'eurusd_darwinex'
    algorithm_info = '%s_test_script' % instrument_pk
    rsi_dqn = RsiDQN(algorithm_info=algorithm_info)
    rsi_dqn.set_parameters(parameters=parameters)

    def test_merge_arrays(self):
        array_1 = np.array(
            [[1, 1, 1, 0.5, 0.15, 2, 2, 2], [1, 1, 2, 0.3, 0.25, 2, 2, 2]]
        )
        array_2 = np.array(
            [[1, 1, 1, 0.65, 0.15, 2, 2, 2], [1, 1, 3, 0.3, 0.25, 2, 2, 2]]
        )
        arrays = [array_1, array_2]
        actions = 2
        state_columns = 3

        df_final = RsiDQN.merge_array_matrix(arrays, state_columns, actions)
        assert len(df_final) == 3

    def test_merge_arrays_negative(self):
        array_1 = np.array(
            [[1, 1, 1, 0.0, 0.0, 2, 2, 2], [1, 1, 2, 0.3, 0.25, 2, 2, 2]]
        )
        array_2 = np.array(
            [[1, 1, 1, -0.65, -0.15, 2, 2, 2], [1, 1, 3, 0.3, 0.25, 2, 2, 2]]
        )
        arrays = [array_1, array_2]
        actions = 2
        state_columns = 3

        df_final = RsiDQN.merge_array_matrix(arrays, state_columns, actions)
        assert len(df_final) == 3
