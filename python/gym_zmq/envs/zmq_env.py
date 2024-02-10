import datetime
import time
from typing import Tuple, Any, Dict

import gymnasium

import numpy as np
from gymnasium.core import ObsType

from gym_zmq.zmq_env_manager import ZmqEnvManager
from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from utils.list_utils import list_value


class ZmqEnv(gymnasium.Env):
    '''
    ZmqEnv is a gymnasium environment that connects to a zeromq server sending and receiving messages in json format.
    https://docs.ray.io/en/latest/rllib-env.html
    '''
    metadata = {'render_modes': ['human']}
    SLEEP_SECONDS_AFTER_DONE = 5  # around 3 seconds in logs

    def __init__(self, env_config: dict):
        import json

        """
        https://docs.ray.io/en/latest/rllib/rllib-env.html

        action_space: The Space object corresponding to valid actions
        observation_space: The Space object corresponding to possible observations
        reward_range: A tuple corresponding to the min and max possible rewards
        Note: a default reward range set to [-inf,+inf] already exists. Set it if you want a narrower range.
        """
        self.zmq_env_manager = None
        if env_config is None:
            raise Exception(rf"error ZmqEnv: env_config is None")
        self.env_config = env_config
        self.initial_reset_is_start = True
        if 'url' not in self.env_config:
            self.env_config['url'] = 'localhost'
        if 'port' not in self.env_config:
            self.env_config['port'] = 5558
        if 'action_space' not in self.env_config:
            raise Exception(
                rf"error ZmqEnv: env_config['action_space'] is None {json.dumps(self.env_config)}"
            )
        if 'observation_space' not in self.env_config:
            raise Exception(
                rf"error ZmqEnv: env_config['observation_space'] is None {json.dumps(self.env_config)}"
            )

        self.zeromq_configuration = ZeroMqConfiguration(
            url=self.env_config['url'], port=self.env_config['port'], protocol='tcp'
        )

        from configuration import USE_IPC_RL_TRAINING
        if USE_IPC_RL_TRAINING:
            algorithm_name = self.env_config.get('algorithm_name', 'localhost')
            ipc_address = rf"{algorithm_name}/{self.env_config['port']}"
            self.zeromq_configuration.set_ipc(ipc_address)

        self.zmq_env_manager = ZmqEnvManager(
            zeromq_configuration=self.zeromq_configuration
        )

        self.action_space = self.env_config['action_space']  # self._action_space()
        self.observation_space = self.env_config[
            'observation_space'
        ]  # self._observation_space()

        self.is_training = self.env_config.get('is_training', False)
        self.iterations = self.env_config.get('iterations', -1)

        self.rewards = []

        self.actions = []
        self.iteration_cum_reward = 0
        self.episode = 0

    def get_empty_state(self):
        return np.zeros(
            self.observation_space.shape[0], dtype=ZmqEnvManager.FLOAT_TYPE
        ).tolist()

    def __del__(self):
        if self.zmq_env_manager is not None:
            self.zmq_env_manager.__del__()

    def init(self):
        # send start to backtest
        if self.render_mode == 'human':
            print(rf"PYTHON: init send start to backtest RENDER")
            from trading_algorithms.algorithm import AlgorithmParameters
            self.env_config[AlgorithmParameters.ui] = 1
        return self.zmq_env_manager.start()

    def step(self, action):
        """
        Get current state of the environment's dynamics.

        Args:
            action (object): an action provided by the agent
        Returns:
            state (object): agent's state of the current environment
            reward (float) : amount of reward returned after previous action
            done (boolean): whether the environment wants to end the agent's eposode, in which case further step() calls will return undefined results
            truncated (boolean): whether the step was truncated
            info (dict): contains auxiliary diagnostic information (helpful for debugging, and sometimes learning)
        """
        # state, reward, done, information
        state, reward, done, truncated, information = self.zmq_env_manager.step(action)

        if state is not None and len(state) > 0:
            state_list = state
            if self.observation_space.shape[0] != len(state_list):
                raise Exception(
                    rf"error ZmqEnv: len(self.observation_space) != len(state_list)"
                )

        self.iteration_cum_reward += reward
        self.actions.append(action)

        if done:
            print(
                rf"done received at iteration {self.zmq_env_manager.iterations} with action {action}  episode: {self.episode}"
            )
            self.rewards.append(self.iteration_cum_reward)

            self.iteration_cum_reward = 0
            self.episode += 1
            time.sleep(self.SLEEP_SECONDS_AFTER_DONE)

        return state, reward, done, truncated, information


    def reset(
            self,
            *,
            seed: int = None,
            options: dict = None,
    ):
        """
        Resets the state of the environment and returns an initial observation.
        When the agent wants to end it's episode, they are responsible for calling this.

        Returns: observation (object): the initial observation of the episode and dict with information

        Parameters
        ----------
        *
        """
        if self.iterations > 0 and self.episode >= self.iterations:
            # over iteration
            print(rf"PYTHON: reset and end of iteration {self.episode}/{self.iterations}")
            state, info = self.get_empty_state(), {}
            return state, info

        start_jar = self.is_training or self.initial_reset_is_start
        if start_jar:
            # start backtest initially always or each reset if bucle_run is False
            if self.episode > 0:
                time.sleep(3)

            self.initial_reset_is_start = False
            print(rf"PYTHON: reset start backtest")
            state, info = self.init()
        elif not self.is_training:
            print(rf"PYTHON: reset its over no more backtest")
            # its over
            state, info = self.get_empty_state(), {}
        else:
            print(rf"PYTHON: reset: send reset to backtest")
            state, info = self.zmq_env_manager.reset()
        self.iteration_cum_reward = 0
        if state is not None and len(state) > 0:
            state_list = state
            if self.observation_space.shape[0] != len(state_list):
                raise Exception(
                    rf"error ZmqEnv: len(self.observation_space) != len(state_list)"
                )

        if isinstance(state, list):
            state = np.array(state)
        return state, info

    def render_all(self, mode='human'):
        self.render(mode=mode)

    def render(self, mode='human', close=False):
        import pandas as pd
        import matplotlib.pyplot as plt

        plt.plot(self.rewards)
        plt.xlabel('episode')
        plt.ylabel('cumulative reward end of episode')
        plt.show()
        # if close:
        # else:
