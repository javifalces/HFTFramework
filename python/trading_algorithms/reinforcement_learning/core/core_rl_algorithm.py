import datetime
from typing import Any, Tuple

import gymnasium
import numpy as np
from gym_zmq import MarketMakingBacktestEnv


class CoreRlAlgorithmEnum:
    baselines3 = "baselines3"
    ray = "ray"


class SaveModelConfig:
    save_model = "save_model"
    save_env = "save_env"
    save_replay_buffer = "save_replay_buffer"
    # save_continuous_action_adaptor = "save_continuous_action_adaptor"#we save it always


class CoreRlAlgorithm:
    @staticmethod
    def create_core(
            core_type: CoreRlAlgorithmEnum,
            algorithm_info: str,
            parameters: dict,
            rl_paths: 'RlPaths'
    ) -> "CoreRlAlgorithm":
        print(rf"CoreRlAlgorithm.create_core: core_type = {core_type}")
        if core_type == CoreRlAlgorithmEnum.baselines3:
            from trading_algorithms.reinforcement_learning.core.baselines3.core_baselines3 import (
                CoreBaselines3,
            )

            return CoreBaselines3(algorithm_info, parameters, rl_paths)
        elif core_type == CoreRlAlgorithmEnum.ray:
            from trading_algorithms.reinforcement_learning.core.ray.core_ray import (
                CoreRay,
            )

            return CoreRay(algorithm_info, parameters, rl_paths)
        else:
            raise ValueError(f"Unknown core type {core_type}")

    def __init__(
            self, algorithm_info: str, parameters: dict, rl_paths: 'RlPaths'
    ) -> None:
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
            BaseModelType,
        )
        import random
        self.parameters = parameters.copy()
        self.algorithm_info = algorithm_info
        self.base_model_str = self.parameters.get(
            RlAlgorithmParameters.model, BaseModelType.DQN
        )
        self.seed = int(
            self.parameters.get(RlAlgorithmParameters.seed, random.randint(0, 10000))
        )
        self.rl_paths = rl_paths
        self.tensorboard_log = self.rl_paths.tensorboard_log
        self.normalize_clip_obs = int(
            self.parameters.get(RlAlgorithmParameters.normalize_clip_obs, 0)
        )
        self.launch_tensorboard = self.parameters.get(RlAlgorithmParameters.launch_tensorboard, True)

    def __reduce__(self):
        return (
            self.__class__,
            (self.state_columns, self.action_columns, self.algorithm_info, self.parameters, self.rl_paths))

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
        raise NotImplementedError

    def test(self, env_config: dict) -> Tuple[Any, gymnasium.Env, list]:
        raise NotImplementedError

    def get_backtest_env(self, env) -> MarketMakingBacktestEnv:
        '''
        ----------
        env

        Returns  the MarketMakingBacktestEnv that is launched nested in other kind of environment
        -------

        '''
        raise NotImplementedError

    def save_model(self, env, model, config=None):
        raise NotImplementedError

    ###### HELPER METHODS ######
    def clean_model(self):
        import os

        files = [
            self.rl_paths.agent_model_path,
            self.rl_paths.normalizer_model_path,
            self.rl_paths.normalizer_json_path,
            self.rl_paths.agent_onnx_path,
            self.rl_paths.continuous_action_adaptor_path,
        ]
        for file in files:
            try:
                model_exists = os.path.exists(file)
                if model_exists:
                    os.remove(file)
            except:
                pass

    def get_model(self):
        raise NotImplementedError
    def _get_number_timesteps_per_episode(self) -> int:
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
        )
        from trading_algorithms.algorithm import AlgorithmParameters

        seconds_per_step = 1
        if (
                RlAlgorithmParameters.step_max_time_waiting_exit_ms in self.parameters
                and RlAlgorithmParameters.step_max_time_waiting_action_results_ms
                in self.parameters
        ):
            miliseconds_per_step = (
                    self.parameters[RlAlgorithmParameters.step_max_time_waiting_exit_ms]
                    * 0.5
                    + self.parameters[
                        RlAlgorithmParameters.step_max_time_waiting_action_results_ms
                    ]
                    * 0.5
            )
            seconds_per_step = np.ceil(miliseconds_per_step / 1000)
        elif RlAlgorithmParameters.step_seconds in self.parameters:
            seconds_per_step = self.parameters[RlAlgorithmParameters.step_seconds]
        else:
            print(
                rf"WARNING: step_seconds not found in parameters , using default value seconds_per_step = {seconds_per_step}"
            )
        number_hours = (
                self.parameters[AlgorithmParameters.last_hour]
                - self.parameters[AlgorithmParameters.first_hour] - 1
        )
        if number_hours <= 0:
            number_hours += 24
        episode_length_seconds = datetime.timedelta(hours=number_hours).total_seconds()
        return episode_length_seconds / seconds_per_step

    def _continuous_action_model(self) -> bool:
        '''
        only used in case is not defined by parameters
        Returns True if the model can operate with continuous actions
        -------

        '''

        from trading_algorithms.reinforcement_learning.rl_algorithm import BaseModelType

        return self.base_model_str in [BaseModelType.DQN, BaseModelType.PPO]
