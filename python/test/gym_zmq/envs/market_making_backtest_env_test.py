import unittest

import gymnasium
import numpy as np

from gym_zmq import MarketMakingBacktestEnv
from gym_zmq.envs import ZmqEnv
from utils.list_utils import list_value


class ZmqEnvTest(unittest.TestCase):
    state_columns = 82
    actions = 16
    low = list_value(
        value=float('-inf'), size=state_columns
    )  # [-1.0, -1.0, -1.0, -1.0]
    high = list_value(value=float('inf'), size=state_columns)  # [1.0, 1.0, 1.0, 1.0]
    observation_space = gymnasium.spaces.Box(
        np.array(low), np.array(high), dtype=np.float32
    )

    input_configuration = """
    {
        "backtest": {
            "startDate": "20231110 00:00:00",
            "endDate": "20231110 23:00:00",
            "bucleRun": false,
            "initialSleepSeconds": -1,
            "instrument": "eurusd_darwinex",
            "delayOrderMs": 0,
            "multithreadConfiguration": "single_thread",
            "feesCommissionsIncluded": false
        },
        "algorithm": {
            "algorithmName": "AlphaMeanReversion__test",
            "parameters": {
                "style": "level",
                "levelToQuotes": [
                    -1
                ],
                "periods": [
                    150,
                    120,
                    50,
                    10
                ],
                "upperBounds": [
                    80,
                    60
                ],
                "upperBoundsExits": [
                    55
                ],
                "lowerBounds": [
                    20,
                    40
                ],
                "lowerBoundsExits": [
                    45
                ],
                "changeSides": [
                    0
                ],
                "maxTimeWaitingActionResultsMs": 1000,
                "maxTimeWaitingExitMs": 50000,
                "candleTypeBusiness": "mid_time_seconds_threshold",
                "volumeCandles": 150000000.0,
                "secondsCandles": 10,
                "stateColumnsFilter": [],
                "binaryStateOutputs": 0,
                "numberDecimalsState": -1,
                "horizonTicksMarketState": 10,
                "periodsTAStates": [
                    3,
                    9,
                    13,
                    21,
                    6,
                    15,
                    19,
                    23,
                    45,
                    90
                ],
                "otherInstrumentsStates": [],
                "otherInstrumentsMsPeriods": [],
                "marketTickMs": 10,
                "dumpFilename": "dump_test.csv",
                "quantity": 0.05,
                "firstHour": 1,
                "lastHour": 22,
                "ui": 0,
                "trainingStats": false,
                "maxBatchSize": 1000000,
                "batchSize": 64,
                "trainingPredictIterationPeriod": -12,
                "trainingTargetIterationPeriod": -14,
                "epoch": 10,
                "learningRateNN": 0.1,
                "learningRateDecrease": false,
                "horizonMinMsTick": 0,
                "scoreEnum": "realized_pnl",
                "epsilon": 0.0,
                "gamma": 0.99,
                "seed": 0,
                "reinforcementLearningActionType": "discrete",
                "stopActionOnFilled": 0,
                "baseModel": "PPO",
                "policy": "MlpPolicy",
                "rlHost": "localhost",
                "rlPort": 12345,
                "device": "auto"
            }
        }
    }
    
    """
    env_config = {
        'port': 12345,
        'action_space': gymnasium.spaces.Discrete(actions),
        'observation_space': observation_space,
        'input_configuration': input_configuration,
    }

    env = MarketMakingBacktestEnv(env_config)

    @unittest.skip("TODO: mock jar answers if not blocking... ")
    def test_check_env_ray(self):
        from ray.rllib.utils.pre_checks.env import check_gym_environments

        check_gym_environments(self.env, config=None)
