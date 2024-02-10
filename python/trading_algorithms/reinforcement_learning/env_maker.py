import datetime


class EnvMaker:
    from gymnasium import Env
    @staticmethod
    def is_port_in_use(port: int) -> bool:
        import socket
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            return s.connect_ex(('localhost', port)) == 0

    @staticmethod
    def get_rl_config(env_config: dict) -> (str, int):

        import json
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
        )
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            DEFAULT_RL_PORT,
            DEFAULT_RL_HOST,
        )

        default_rl_port = DEFAULT_RL_PORT
        default_rl_host = DEFAULT_RL_HOST
        if 'input_configuration' in env_config:
            input_configuration = env_config['input_configuration']
            input_configuration_dict = json.loads(input_configuration)
            if (
                    'algorithm' in input_configuration_dict
                    and 'parameters' in input_configuration_dict['algorithm']
            ):
                algorithm_parameters = input_configuration_dict['algorithm'][
                    'parameters'
                ]
                if RlAlgorithmParameters.rl_port in algorithm_parameters:
                    default_rl_port = algorithm_parameters[
                        RlAlgorithmParameters.rl_port
                    ]
                if RlAlgorithmParameters.rl_host in algorithm_parameters:
                    default_rl_host = algorithm_parameters[
                        RlAlgorithmParameters.rl_host
                    ]

        return default_rl_host, default_rl_port

    @staticmethod
    def set_eval_env_config(env_config: dict, rl_host: str, rl_port: int,
                            start_date_validation: datetime.datetime = None,
                            end_date_validation: datetime.datetime = None) -> dict:
        import json
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
        )
        is_validation_date_config = start_date_validation is not None and end_date_validation is not None

        if 'input_configuration' in env_config:
            input_configuration = env_config['input_configuration']
            input_configuration_dict = json.loads(input_configuration)
            if (
                    'algorithm' in input_configuration_dict
                    and 'parameters' in input_configuration_dict['algorithm']
            ):
                name = input_configuration_dict['algorithm']['algorithmName']
                validation_name = rf"{name}_validation"
                input_configuration_dict['algorithm']['algorithmName'] = validation_name
                algorithm_parameters = input_configuration_dict['algorithm'][
                    'parameters'
                ]
                algorithm_parameters[RlAlgorithmParameters.rl_port] = rl_port
                algorithm_parameters[RlAlgorithmParameters.rl_host] = rl_host
                # remove tb log too
                algorithm_parameters[RlAlgorithmParameters.training_stats] = False
                algorithm_parameters[RlAlgorithmParameters.launch_tensorboard] = False

            # change date if its not None, None only in parameter tuning
            if is_validation_date_config and 'backtest' in input_configuration_dict:
                backtest_parameters = input_configuration_dict['backtest']
                from backtest.input_configuration import format_date_hour
                backtest_parameters['startDate'] = start_date_validation.strftime(format_date_hour)
                backtest_parameters['endDate'] = end_date_validation.strftime(format_date_hour)

            input_configuration_str = json.dumps(input_configuration_dict)
            env_config['input_configuration'] = input_configuration_str

        return env_config

    @staticmethod
    def _get_algorithm_name(env_config: dict) -> str:
        import json

        algorithm_name = 'default_algorithm_name'
        if 'input_configuration' in env_config:
            input_configuration = env_config['input_configuration']
            input_configuration_dict = json.loads(input_configuration)
            if (
                    'algorithm' in input_configuration_dict
                    and 'algorithmName' in input_configuration_dict['algorithm']
            ):
                algorithm_name = input_configuration_dict['algorithm']['algorithmName']

        return algorithm_name

    @staticmethod
    def _get_seed(env_config: dict) -> int:
        import json
        import random
        from trading_algorithms.algorithm import AlgorithmParameters

        seed = random.randint(0, 1000000)
        if 'input_configuration' in env_config:
            input_configuration = env_config['input_configuration']
            input_configuration_dict = json.loads(input_configuration)
            if (
                    'algorithm' in input_configuration_dict
                    and 'parameters' in input_configuration_dict['algorithm']
            ):
                algorithm_parameters = input_configuration_dict['algorithm'][
                    'parameters'
                ]
                if AlgorithmParameters.seed in algorithm_parameters:
                    seed = algorithm_parameters[AlgorithmParameters.seed]
        return seed

    def __init__(self, env_config: dict, counter: int):
        from gym_zmq import MarketMakingBacktestEnv
        import numpy as np
        import copy

        self.env_config_original = copy.copy(env_config)

        self.counter = counter
        port_delta_counter = 2  # min 2 because of EvalCallback in the next port configured!
        initial_sleep_delta_counter = 0  # 5
        batch_sleep = 3

        default_rl_host, default_rl_port = EnvMaker.get_rl_config(env_config)
        algorithm_name = EnvMaker._get_algorithm_name(env_config)
        seed = EnvMaker._get_seed(env_config)
        rl_port = default_rl_port

        if self.counter > 0:
            algorithm_name = rf"{algorithm_name}_{self.counter}"
            seed = seed + self.counter
            initial_sleep_seconds = initial_sleep_delta_counter * np.floor(
                self.counter / batch_sleep
            )
            print(
                rf"{algorithm_name} add initial sleep seconds in index {self.counter} of {initial_sleep_seconds} seconds"
            )
            env_config['initial_sleep_seconds'] = initial_sleep_seconds
            rl_port += self.counter * port_delta_counter

        self.env_config = MarketMakingBacktestEnv.modify_env_config_rl(
            env_config,
            rl_port=rl_port,
            rl_host=default_rl_host,
            algorithm_name=algorithm_name,
            seed=seed,
        )
        self.env_config['thread_name'] = algorithm_name

    def create(self) -> Env:
        from gymnasium import make
        from gym_zmq import MarketMakingBacktestEnv

        return make(
            id=MarketMakingBacktestEnv.ENVIRONMENT_ID, env_config=self.env_config
        )

    def __copy__(self):
        newone = type(self)(self.env_config, self.counter)
        newone.__dict__.update(self.__dict__)
        return newone

    def copy(self):
        return self.__copy__()

    def __reduce__(self):
        return (
            self.__class__,
            (self.env_config_original, self.counter),
        )
