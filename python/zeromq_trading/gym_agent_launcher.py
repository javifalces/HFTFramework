import datetime

from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from market_data_feed.zeromq_live.zeromq_connector import ZeroMqRequester
import os

from trading_algorithms.reinforcement_learning.continuous_action_adaptor import ContinuousActionAdaptor
from trading_algorithms.reinforcement_learning.rl_algorithm import RLAlgorithm, ReinforcementLearningActionType, \
    RlAlgorithmParameters, BaseModelType
from utils.list_utils import list_to_numpy
import numpy as np
import json
import time


class GymAgentLauncher:
    def __init__(self, rl_host: str, rl_port: int, model_path: str, normalizer_model_path: str,
                 base_model_str: str, action_adaptor_path: str = None,
                 reinforcement_learning_action_type: ReinforcementLearningActionType = None):

        self.initial_sleep_seconds = 10
        self.rl_host = rl_host
        self.rl_port = rl_port
        self.model_path = model_path
        self.normalizer_model_path = normalizer_model_path
        self.action_adaptor_path = action_adaptor_path

        self.continuous_action_adaptor: ContinuousActionAdaptor = None

        self.model = None
        self.normalizer = None

        from trading_algorithms.reinforcement_learning.core.core_rl_algorithm import CoreRlAlgorithm, \
            CoreRlAlgorithmEnum
        parameters = {
            RlAlgorithmParameters.model: base_model_str,
        }
        self.core_rl = CoreRlAlgorithm.create_core(core_type=CoreRlAlgorithmEnum.baselines3,
                                                   algorithm_info="",
                                                   parameters=parameters)

        self.base_model = self.core_rl.get_model()
        if reinforcement_learning_action_type is None:
            self.reinforcement_learning_action_type = RLAlgorithm.get_reinforcement_learning_action_type(
                self.base_model)
        else:
            self.reinforcement_learning_action_type = reinforcement_learning_action_type

        self.zeromq_configuration = ZeroMqConfiguration(url=self.rl_host, port=self.rl_port)
        self.zeromq_requester = ZeroMqRequester(zeromq_configuration=self.zeromq_configuration)

        print(rf"GymAgentLauncher connecting to {self.zeromq_configuration.url}:{self.zeromq_configuration.port}")

    def __del__(self):
        self.zeromq_requester.context.destroy()

    def _load(self):
        import pickle
        if (not os.path.exists(self.model_path)):
            raise Exception(rf"Model path {self.model_path} does not exist")
        print(rf"Loading model from {self.model_path}")
        self.model = self.base_model.load(self.model_path)
        if os.path.exists(self.normalizer_model_path):
            print(rf"Loading normalizer from {self.normalizer_model_path}")
            with open(self.normalizer_model_path, "rb") as file_handler:
                self.normalizer = pickle.load(file_handler)
            # self.normalizer = VecNormalize.load(self.normalizer_model_path)

        if self.reinforcement_learning_action_type == ReinforcementLearningActionType.continuous:
            if self.action_adaptor_path is None:
                raise Exception(rf"Action adaptor path is None in continuous action type {self.base_model}")

            if not os.path.exists(
                    self.action_adaptor_path):
                raise Exception(
                    rf"Action adaptor path {self.action_adaptor_path} does not exist in continuous action type in {self.base_model}")

            if os.path.exists(
                    self.action_adaptor_path):
                print(rf"Loading Action adaptor from {self.action_adaptor_path}")
                self.continuous_action_adaptor = ContinuousActionAdaptor.load(self.action_adaptor_path)

    def _step(self, action):
        if self.continuous_action_adaptor is not None:
            action = self.continuous_action_adaptor.adapt_action(action)

        observation = np.zeros(shape=(1, 1), dtype=np.float32)
        reward = float(0.0)
        done = True
        info = {}

        message_json = {}
        message_json['type'] = 'action'
        action_arr = np.asarray(action)
        if action_arr.ndim == 0:
            message_json['value'] = action_arr.tolist()
        else:
            message_json['value'] = action_arr.tolist()
        message = json.dumps(message_json)
        reply = self.zeromq_requester.send_request(message, send_request_retries=10, receive_timeout_ms=60000)
        if reply:
            if reply == "KO":
                print("WARNING: KO reply => reset")
                reply_dict = {}
                reply_dict['done'] = True
                reply_dict['reward'] = 0.0
                reply_dict['info'] = ""
            else:
                reply_dict = json.loads(reply)

            done = reply_dict['done']
            reward = reply_dict['reward']
            info = reply_dict['info']
            if 'state' not in reply_dict:
                reply_dict['state'] = []
            observation = list_to_numpy(reply_dict['state'])
        # observation, reward, done, information
        truncated = False  # for what?
        return observation, reward, done, truncated, info

    def start(self):
        time.sleep(self.initial_sleep_seconds)
        self._load()
        action = self.model.action_space.sample()
        iterations = 0
        is_ready = False
        start_time = datetime.datetime.utcnow()
        elapsed_last_not_ready_seconds = 70
        elapsed_last_start = datetime.datetime.utcnow()
        while True:
            elapsed_not_ready = datetime.datetime.utcnow() - start_time
            if not is_ready and elapsed_last_not_ready_seconds > 60:
                elapsed_last_start = datetime.datetime.utcnow()
                print(
                    rf"{datetime.datetime.utcnow()}  waiting algorithm is ready {round(elapsed_not_ready.total_seconds() / 60.0, 2)} minutes ... since {start_time}")
            if is_ready:
                print(rf"{datetime.datetime.utcnow()}  action: {action} iterations: {iterations}")
            observation, reward, done, truncated, info = self._step(action)
            info_message = info.get('message', '')
            is_ready = info_message != 'not_ready'
            if not is_ready:
                time.sleep(3)  # to give some space to cpu
                elapsed_last_not_ready_seconds = (datetime.datetime.utcnow() - elapsed_last_start).total_seconds()
                continue

            if info_message == 'error':
                # not ready in the other side
                print(rf"ERROR: {info.get('error', '')}")
                continue
            action, _states = self.get_action_state(observation)

            iterations += 1

    def get_action_state(self, observation):
        if self.normalizer is not None:
            observation = self.normalizer.normalize_obs(observation)
        action, _states = self.model.predict(observation)
        return action, _states


import sys
import argparse

if __name__ == '__main__':
    # localhost 1212 X:\output_models\DQNRSISideQuoting_eurusd_darwinex_test\agent_model.zip X:\output_models\DQNRSISideQuoting_eurusd_darwinex_test\normalizer_model.pkl DQN discrete path
    try:
        if len(sys.argv) < 7:
            print(
                "Usage: python gym_agent_launcher.py <rl_host> <rl_port> <model_path> <normalizer_model_path> <base_model> <reinforcement_learning_action_type> <action_adaptor_path>")
            sys.exit(1)

        parser = argparse.ArgumentParser("GymAgentLauncher")
        parser.add_argument("rl_host", help="", type=str, default="localhost")
        parser.add_argument("rl_port", help="", type=int, default=-1)
        parser.add_argument("model_path", help="agent model path", type=str)
        parser.add_argument("normalizer_model_path", help="", type=str)
        parser.add_argument("base_model", help="DQN,PPO,SAC...", type=str, default="DQN")
        parser.add_argument("reinforcement_learning_action_type", help="reinforcement_learning_action_type", type=str)
        parser.add_argument("action_adaptor_path", help="action_adaptor_path", type=str)

        args = parser.parse_args()
        launcher = GymAgentLauncher(rl_host=args.rl_host, rl_port=args.rl_port, model_path=args.model_path,
                                    normalizer_model_path=args.normalizer_model_path, base_model_str=args.base_model,
                                    action_adaptor_path=args.action_adaptor_path,
                                    reinforcement_learning_action_type=args.reinforcement_learning_action_type)
        launcher.start()
    except Exception as e:
        print(rf"ERROR: {e} ")
        input("Press Enter to exit...")
