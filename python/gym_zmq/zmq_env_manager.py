import json
import time
from typing import Tuple

import numpy as np

from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from market_data_feed.zeromq_live.zeromq_connector import ZeroMqRequester
from utils.list_utils import list_to_numpy, list_to_str


class ZmqEnvManager:
    FLOAT_TYPE = np.float32  # required for ray
    INT_TYPE = np.int32
    STEP_TIMEOUT_MILISECONDS = 30000  # -1 is not waiting
    BACKTEST_IS_READY_TIMEOUT_SECONDS = 5 * 60  # more than 5 mins is an error
    RESET_BACKTEST_TIMEOUT_SECONDS = 2 * 60  # more than 5 mins is an error
    FORCE_PAUSE_ITERATION = -1
    FORCE_PAUSE_SECONDS = 35
    def __init__(
            self,
            zeromq_configuration: ZeroMqConfiguration = ZeroMqConfiguration(
                url='localhost', port=5558
            ),
    ):
        """
        https://docs.ray.io/en/latest/rllib/rllib-env.html

        action_space: The Space object corresponding to valid actions
        observation_space: The Space object corresponding to possible observations
        reward_range: A tuple corresponding to the min and max possible rewards
        Note: a default reward range set to [-inf,+inf] already exists. Set it if you want a narrower range.
        """
        self.zeromq_requester = ZeroMqRequester(
            zeromq_configuration=zeromq_configuration
        )
        self.iterations = 0
        self.last_state = None

    def reset_zmq_connector(self):
        self.zeromq_requester.disconnect(1)

        self.zeromq_requester = ZeroMqRequester(
            zeromq_configuration=self.zeromq_requester.zeromq_configuration
        )

    def __del__(self):
        if self.zeromq_requester is not None:
            self.zeromq_requester.socket.close()

    @staticmethod
    def _invalid_float(input: float) -> bool:
        return np.isnan(input) or np.isinf(input) or input is None or np.isfinite(input) is False

    def step(self, action):
        """
        Get current state of the environment's dynamics.

        Args:
            action (object): an action provided by the agent
        Returns:
            observation (object): agent's observation of the current environment
            reward (float) : amount of reward returned after previous action
            done (boolean): whether the environment wants to end the agent's eposode, in which case further step() calls will return undefined results
            info (dict): contains auxiliary diagnostic information (helpful for debugging, and sometimes learning)
        """

        if self.FORCE_PAUSE_ITERATION > 0 and self.iterations > self.FORCE_PAUSE_ITERATION:
            print(rf"FORCE_PAUSE_ITERATION force sleeping step {self.FORCE_PAUSE_SECONDS} seconds")
            time.sleep(self.FORCE_PAUSE_SECONDS)
            print(rf"continue...")
            self.FORCE_PAUSE_ITERATION = -1

        self.iterations += 1
        return self._request("action", action)

    def reset(self):
        """
        Resets the state of the environment and returns an initial observation.
        When the agent wants to end it's episode, they are responsible for calling this.

        Returns: observation (object): the initial observation of the episode
        """
        print("PYTHON: reset sent")
        observation, reward, done, truncated, info = self._request("reset")
        self.iterations = 0
        return observation, info

    def start_backtest(self):
        print("PYTHON: backtest_is_ready sent. Waiting backtestIsReady...")
        observation, reward, done, truncated, info = self._request("backtest_is_ready")

        print(f"PYTHON: backtest_is_ready finished {info['message']}")
        if 'ERROR' in info['message']:
            return False
        return True

    def start(self):
        print("PYTHON: start sent")
        observation, reward, done, truncated, info = self._request("start")
        return observation, info

    def _request_step(self, message_json: dict, value: list) -> Tuple[dict, int, int]:
        request_retries = 1
        response_timeout = ZmqEnvManager.STEP_TIMEOUT_MILISECONDS  #-1
        message_json['type'] = 'action'
        action_arr = np.asarray(value)
        if action_arr.ndim == 0:
            message_json['value'] = action_arr.tolist()
        else:
            message_json['value'] = action_arr.tolist()
        return message_json, request_retries, response_timeout

    def _request(self, type: str, value: list = None):
        # observation = np.zeros(shape=self.observation_space.shape, dtype=self.observation_space.dtype)
        observation = np.zeros(shape=(1, 1), dtype=ZmqEnvManager.FLOAT_TYPE)
        reward = ZmqEnvManager.FLOAT_TYPE(0.0)
        done = True
        info = {}
        message_json = {}
        # print("zmq_env: action {}".format(action))
        message_json['type'] = type
        message_json['value'] = []
        request_retries = 1
        request_timeout_ms = -1

        is_step_request = value is not None
        if is_step_request:
            message_json, request_retries, request_timeout_ms = self._request_step(
                message_json, value
            )
        if type == "backtest_is_ready":
            request_retries = 1
            request_timeout_ms = (
                    ZmqEnvManager.BACKTEST_IS_READY_TIMEOUT_SECONDS * 1000
            )  # time to read parquets ,no more than 100 seconds

        if type == "reset":
            request_retries = 1
            request_timeout_ms = (
                    ZmqEnvManager.RESET_BACKTEST_TIMEOUT_SECONDS * 1000
            )  # time to read parquets ,no more than 100 seconds

        message = json.dumps(message_json)
        reply = self.zeromq_requester.send_request(
            message=message,
            send_request_retries=request_retries,
            receive_timeout_ms=request_timeout_ms,
        )

        if reply:
            if reply.startswith("KO"):
                print(rf"WARNING: KO reply {reply} => reset")
                reply_dict = {}
                reply_dict['done'] = True
                reply_dict['reward'] = ZmqEnvManager.FLOAT_TYPE(0.0)
                reply_dict['info'] = {}  # must be a dict
                reply_dict['info']['message'] = f"ERROR : {reply}"
            else:
                reply_dict = json.loads(reply)

            # print("zmq_env: Server replied {}".format(reply))
            done = reply_dict['done']
            reward = reply_dict['reward']

            if self.iterations <= 1:
                # 0 is reset/start
                # 1 is first reply and we are not state ready => > 5 seconds
                # print(rf"ignore first reply {self.iterations} reward! {value} ")
                reward = ZmqEnvManager.FLOAT_TYPE(0.0)

            info = reply_dict['info']
            if 'state' not in reply_dict:
                if self.last_state is not None:
                    reply_dict['state'] = np.zeros(len(self.last_state)).tolist()
                else:
                    reply_dict['state'] = []
            else:
                self.last_state = reply_dict['state']
            observation = list_to_numpy(reply_dict['state']).astype(
                ZmqEnvManager.FLOAT_TYPE
            )
            reward = ZmqEnvManager.FLOAT_TYPE(reward)
        # observation, reward, done, information
        truncated = False  # for what?
        import math
        observation = [i if math.isnan(i) == False and math.isinf(i) == False else 0 for i in
                       observation]  # replace all nan and inf values in the list by 0
        if self._invalid_float(reward):
            print(
                rf"WARNING: NAN/inf/not finite in reward received at iteration {self.iterations} with action {value} -> 0.0")
            reward = 0.0

        if isinstance(observation, list):
            observation = np.array(observation)

        return observation, reward, done, truncated, info
