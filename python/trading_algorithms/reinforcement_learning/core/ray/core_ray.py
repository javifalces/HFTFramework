import datetime
from pathlib import Path
from typing import Tuple, Any

import gymnasium

from gym_zmq import MarketMakingBacktestEnv
from trading_algorithms.reinforcement_learning.core.core_rl_algorithm import (
    CoreRlAlgorithm,
)
import os
from ray import tune
from ray.train import CheckpointConfig, Checkpoint
from ray.rllib.algorithms.algorithm import Algorithm

from trading_algorithms.reinforcement_learning.rl_algorithm import (
    InfoStepKey,
    RLAlgorithm,
    BaseModelType,
    RlAlgorithmParameters,
)


class CoreRay(CoreRlAlgorithm):
    def __init__(
            self, algorithm_info: str, parameters: dict, rl_paths: 'RlPaths'
    ) -> None:
        super().__init__(algorithm_info, parameters, rl_paths)
        self.env = None  # will have to set it later
        self._base_model = None

    def load_model(self):
        import os

        if os.path.isdir(self.rl_paths.agent_model_checkpoint_path):
            print(
                rf"CoreRay.load_model: Loading model from {self.rl_paths.agent_model_checkpoint_path}"
            )
            self._base_model = Algorithm.from_checkpoint(
                self.rl_paths.agent_model_checkpoint_path
            )

    def get_model(self):
        raise NotImplementedError
    def save_model(self, env, model, config=None):
        if not os.path.isdir(self.rl_paths.agent_model_checkpoint_path):
            Path(self.rl_paths.agent_model_checkpoint_path).mkdir(parents=True)
        model.save_checkpoint(self.rl_paths.agent_model_checkpoint_path)

    def check_model(self, model, env) -> bool:
        assert isinstance(model, Algorithm)
        # TODO add more checks state and action space
        return True

    def train(
            self,
            env_config: dict,
            iterations: int,
            simultaneous_algos: int,
            score_early_stopping: str,
            patience: int,
            clean_initial_experience: bool = False,
            min_iterations: int = None,
            plot_training: bool = True,
            tb_log_name: str = None,
            start_date_validation: datetime.datetime = None,
            end_date_validation: datetime.datetime = None,
            use_validation_callback: bool = False,
    ) -> Tuple[Any, gymnasium.Env, list]:

        if clean_initial_experience:
            self.clean_model()

        config, stop = self._get_config(
            env_config,
            iterations,
            simultaneous_algos,
            score_early_stopping,
            patience,
            min_iterations,
            tb_log_name,
        )

        checkpoint_config = CheckpointConfig(checkpoint_at_end=True)

        experiment = tune.run(
            self.base_model_str,
            config=config,
            stop=stop,
            checkpoint_config=checkpoint_config,
        )
        # Get the best result based on a particular metric.
        best_result = experiment.get_best_trial(
            metric="episode_reward_mean", mode="max"
        )
        # Get the best checkpoint corresponding to the best result.
        best_checkpoint = best_result.checkpoint
        agent = Algorithm.from_checkpoint(best_checkpoint)
        return agent, None, experiment

    def test(self, env_config: dict) -> Tuple[Any, gymnasium.Env]:
        self.load_model()
        if self._base_model is None:
            print(rf"WARNING: CoreRay.test: self._base_model is None -> create new one")
            config, stop = self._get_config(env_config, 1, 1)
            from ray.rllib.algorithms import AlgorithmConfig

            algorithm_config = AlgorithmConfig(
                algo_class=self.base_model_str
            ).from_dict(config)
            self._base_model = Algorithm(algorithm_config)

        env = MarketMakingBacktestEnv(env_config=env_config)
        episode_reward = 0
        done = False
        obs, info = env.reset(seed=self.seed)
        while not done:
            action = self._base_model.compute_single_action(obs)
            obs, reward, done, info, truncated = env.step(action)
            episode_reward += reward
            env.render()

        return self._base_model, env

    def get_backtest_env(self, env) -> MarketMakingBacktestEnv:
        '''
        ----------
        env

        Returns  the MarketMakingBacktestEnv that is launched nested in other kind of environment
        -------

        '''
        # TODO: check it
        return env

    ######## HELPERS ########
    def _get_config(
            self,
            env_config: dict,
            iterations: int,
            simultaneous_algos: int = 1,
            score_early_stopping: str = InfoStepKey.totalPnl,
            patience: int = -1,
            min_iterations: int = None,
            tb_log_name: str = None,
    ) -> (dict, dict):
        import json
        from trading_algorithms.reinforcement_learning.core.ray.core_ray_callbacks import (
            CustomCallbacks,
        )
        from trading_algorithms.reinforcement_learning.env_maker import EnvMaker

        if simultaneous_algos is None or simultaneous_algos == 0:
            simultaneous_algos = 1
        if simultaneous_algos < 1:
            # get the number of processors minus simultaneous_algos
            simultaneous_algos = os.cpu_count() - simultaneous_algos
        simultaneous_algos = min(simultaneous_algos, os.cpu_count())
        if simultaneous_algos == 1:
            env_configs = [EnvMaker(env_config, counter=i) for i in range(1)]
        else:
            env_configs = [
                EnvMaker(env_config, counter=i) for i in range(simultaneous_algos)
            ]

        # TODO:fix for simultaneous_algos>1
        for env_config_iter in env_configs:
            env = MarketMakingBacktestEnv
            env_config = env_config_iter.env_config
            config = {
                "env": env,
                "env_config": env_config,
                # "disable_env_checking": True,
                # "num_gpus": 0,
                "num_workers": simultaneous_algos,
                "num_envs_per_worker ": 1,
                "framework": "torch",
                "callbacks": CustomCallbacks,
            }

            stop = {
                "episodes_total": iterations,
            }

        return config, stop


def main():
    import gymnasium
    import gymnasium as gym
    from ray.train import CheckpointConfig, Checkpoint
    from ray.rllib.algorithms.algorithm import Algorithm

    from ray import tune

    from gym_zmq import MarketMakingBacktestEnv
    from utils.list_utils import list_value
    import numpy as np

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

    config = {
        "env": MarketMakingBacktestEnv,
        "env_config": env_config,
        # "disable_env_checking": True,
        "num_gpus": 0,
        "num_workers": 1,
        "num_envs_per_worker ": 1,
        "framework": "torch",
    }
    import os

    os.environ["LOG_STATE_STEPS"] = "1"  # print each step state in logs

    # https://docs.ray.io/en/latest/tune/api_docs/execution.html#tune-run
    stop = {
        "episodes_total": 3,
    }

    # https://docs.ray.io/en/latest/train/api/doc/ray.train.CheckpointConfig.html
    checkpoint_config = CheckpointConfig(checkpoint_at_end=True)

    base_model_str = 'PPO'
    import ray
    from ray.rllib.algorithms import ppo

    experiment = tune.run(
        base_model_str, config=config, stop=stop, checkpoint_config=checkpoint_config
    )

    # Get the best result based on a particular metric.
    best_result = experiment.get_best_trial(metric="episode_reward_mean", mode="max")
    # Get the best checkpoint corresponding to the best result.
    best_checkpoint = best_result.checkpoint

    agent = Algorithm.from_checkpoint(best_checkpoint)
    print(best_checkpoint.path)
    # ## TEST
    # best_checkpoint_read = Checkpoint(
    #     path='C:/Users/javif/ray_results/PPO_2023-12-31_09-00-49/PPO_CartPole-v1_b5e26_00000_0_2023-12-31_09-00-49/checkpoint_000000')
    # agent = Algorithm.from_checkpoint(best_checkpoint_read)
    # from ray.tune.registry import register_env
    #
    #
    # env = gym.make("CartPole-v1", render_mode="rgb_array")
    # episode_reward = 0
    # done = False
    #
    # obs, info = env.reset()
    #
    # while not done:
    #     action = agent.compute_single_action(obs)
    #     obs, reward, done, info, truncated = env.step(action)
    #     episode_reward += reward
    #     env.render()


if __name__ == '__main__':
    main()
