import copy
from threading import Thread

from stable_baselines3 import A2C, DQN, PPO, SAC, TD3
from stable_baselines3.common.vec_env import DummyVecEnv, SubprocVecEnv

from backtest.backtest_launcher import (
    BacktestLauncher,
    BacktestLauncherController,
    ListenerKill,
)
from backtest.input_configuration import (
    InputConfiguration,
    BacktestConfiguration,
    AlgorithmConfiguration,
    JAR_PATH,
)
from gym_zmq.envs import ZmqEnv
import json

from gym_zmq.zmq_env_manager import ZmqEnvManager
from market_data_feed.zeromq_live.zeromq_configuration import ZeroMqConfiguration
from utils.list_utils import list_value
import time


class MarketMakingBacktestEnv(ZmqEnv, ListenerKill):
    '''
    Market making backtest environment: launch the jar with the backtest configuration and connect to it
    '''

    ENVIRONMENT_ID = 'MarketMakingBacktestEnv'
    INIT_TRIALS_ERROR = 3
    MAX_TIMEOUT_RESET_SECONDS = 60 * 3  # 3 mins max to clsoe the process , if not....

    @staticmethod
    def register_ray(environment_id: str = ENVIRONMENT_ID):
        from ray.tune.registry import register_env

        def env_creator(env_config):
            return MarketMakingBacktestEnv(
                env_config=env_config
            )  # return an env instance

        register_env(environment_id, env_creator)

    @staticmethod
    def register(environment_id: str = ENVIRONMENT_ID, env_config_default: dict = {}):
        import gymnasium

        gymnasium.register(
            id=environment_id,
            entry_point='gym_zmq.envs.market_making_backtest_env:MarketMakingBacktestEnv',
            kwargs={'env_config': env_config_default},
            # disable_env_checker=False,
            # apply_api_compatibility=True
        )

    @staticmethod
    def modify_env_config_rl(
            env_config: dict,
            rl_host: str = None,
            rl_port: int = None,
            algorithm_name: str = None,
            seed: int = None,
    ) -> (dict):
        env_config_out = env_config.copy()
        input_configuration = InputConfiguration.read_json(
            env_config_out['input_configuration']
        )

        if rl_port is not None and (rl_host is None or rl_host == 'localhost'):
            from trading_algorithms.reinforcement_learning.env_maker import EnvMaker
            while EnvMaker.is_port_in_use(rl_port):
                print(f"WARNING: modify_env_config_rl localhost port {rl_port} in use, incrementing")
                rl_port = rl_port + 1


        input_configuration_out = InputConfiguration.copy_from_input_configuration(
            input_configuration,
            rl_host=rl_host,
            rl_port=rl_port,
            algorithm_name=algorithm_name,
            seed=seed,
        )
        env_config_out['input_configuration'] = input_configuration_out.get_json()
        # this is for python side

        if rl_port is not None:
            env_config_out['port'] = rl_port

        if rl_host is not None:
            env_config_out['url'] = rl_host

        return env_config_out

    def __init__(self, env_config: dict, initialized: bool = False, jar_is_killed: bool = False,
                 initial_trial_error: int = 0, wait_jar_is_ready: bool = False):
        super().__init__(env_config)
        self.backtest_launcher = None
        self.initial_trial_error = initial_trial_error
        self.jar_is_killed = jar_is_killed
        if 'input_configuration' not in self.env_config:
            raise Exception(
                rf"ERROR MarketMakingBacktestEnv : env_config['input_configuration'] is None"
            )

        self.initial_sleep_seconds = 0
        # self.thread_name = ''
        self.continuous_action_adaptor = self.env_config.get(
            'continuous_action_adaptor', None
        )

        self.thread_name = self.env_config.get('thread_name', "default_thread_name")

        if 'initial_sleep_seconds' in self.env_config:
            self.initial_sleep_seconds = self.env_config['initial_sleep_seconds']

        self.input_configuration_str = self.env_config['input_configuration']
        # serialize self.input_configuration_str into a InputConfiguration object
        self.input_configuration = InputConfiguration.read_json(
            self.input_configuration_str
        )
        self.id = self.input_configuration.algorithm_configuration.algorithm_name

        self.wait_jar_is_ready = wait_jar_is_ready
        self.initialized = initialized

    def __reduce__(self):
        return (
            MarketMakingBacktestEnv,
            (
                self.env_config,
                self.initialized,
                self.jar_is_killed,
                self.initial_trial_error,
                self.wait_jar_is_ready,
            ),
        )

    def __deepcopy__(self, memo):
        from copy import copy, deepcopy

        cls = self.__class__
        result = cls.__new__(cls)
        memo[id(self)] = result
        for k, v in self.__dict__.items():
            setattr(result, k, deepcopy(v, memo))
        return result

    def __str__(self):
        return rf"{MarketMakingBacktestEnv.ENVIRONMENT_ID}_{self.id}_{self.zeromq_configuration.port}"

    def reset(
            self,
            *,
            seed: int = None,
            options: dict = None,
    ):
        print(rf"market_making_backtest_env {self.id} reset received")
        self.initialized = False
        return super().reset(seed=seed, options=options)

    def notify_kill(self, code):
        print(rf"MarketMakingBacktestEnv {self.id} notify_kill code = {code}")
        # zero_mq_requester = self.zmq_env.rl_manager.zeromq_requester
        # zero_mq_requester.socket.close()
        # zero_mq_requester.disconnect(timeout_seconds=15)
        # self.zmq_env.rl_manager.reset_zmq_connector()
        # self.zmq_env.rl_manager.zeromq_requester.connect()
        try:
            self.jar_is_killed = True
            # maybe backtest_is_ready is send and blocked in requester waiting to be answered
            # zero_mq_requester = self.zmq_env.rl_manager.zeromq_requester
            # zero_mq_requester.disconnect(timeout_seconds=15)
        except:
            pass
        pass

    def _start_backtest(self):
        import copy

        self.backtest_launcher = BacktestLauncher(
            input_configuration=copy.copy(self.input_configuration),
            id=self.id,
            jar_path=JAR_PATH,
        )
        self.backtest_launcher.register_kill_listener(self)
        # starting in another thread
        self.backtest_controller = BacktestLauncherController(
            backtest_launchers=[self.backtest_launcher], max_simultaneous=1
        )
        # start the thread but get the output from run method in a variable

        self.jar_thread = Thread(
            target=self.backtest_controller.run, name=self.thread_name
        )
        if self.initial_sleep_seconds > 0:
            print(
                rf"start_backtest {self.id} launching jar... waiting {self.initial_sleep_seconds} seconds on  episode = "
                rf"{self.episode} {self.zeromq_configuration}"
            )
            time.sleep(self.initial_sleep_seconds)
        else:
            print(rf"start_backtest {self.id} launching jar...")

        self.wait_jar_is_ready = False
        # reset zero_mq_connector if enabled
        self.zmq_env_manager.reset_zmq_connector()

        self.jar_is_killed = False
        self.jar_thread.start()
        # sleep 10 seconds to wait for the jar to start
        return self._wait_start_jar()

    def _wait_start_jar(self):

        print(
            rf"start_backtest {self.id} waiting (backtest_is_ready) starting jar... on  episode = {self.episode} {self.zeromq_configuration}"
        )
        started = self.zmq_env_manager.start_backtest()
        if started:
            print(
                rf"start_backtest wait {self.id} finished -> send start {self.zeromq_configuration} "
            )
        else:
            print(rf"start_backtest {self.id} wait finished with errors")
        return started

    def wait_finished(self):
        if self.jar_thread is not None:
            self.jar_thread.join()

    def get_last_controller_output(self) -> dict:
        self.wait_finished()
        output = self.backtest_controller.last_output
        return output

    def _stop_backtest(self):
        if (
                self.backtest_launcher is not None
                and self.backtest_launcher.proc is not None
        ):
            print(rf"stop_backtest {self.id} sending reset... on  episode = {self.episode}")
            state, info = self.zmq_env_manager.reset()
            if 'ERROR' in info['message']:
                print(
                    rf"WARNING: stop_backtest closing process {self.id} on  episode = {self.episode} -> force close"
                )
                self.force_close()

            else:
                print(rf"_stop_backtest waiting closing process {self.id} on episode = {self.episode}")
                if self.backtest_launcher.proc is not None:
                    try:
                        ret = self.backtest_launcher.proc.wait(
                            timeout=self.MAX_TIMEOUT_RESET_SECONDS
                        )
                    except Exception:
                        print(
                            rf"WARNING: timeout _stop_backtest waiting closing process {self.id} on episode = {self.episode} -> force_close")
                        self.force_close()

                else:
                    print(rf"WARNING: _stop_backtest can't wait close process {self.id} on episode = {self.episode}")
                    self.force_close()

        else:
            print(rf"stop_backtest {self.id} sending not launcher on episode = {self.episode}")
            self.initialized = False

    def close(self):
        print(rf"market_making_backtest_env {self.id} close received")
        self._stop_backtest()

    def force_close(self):
        print(rf"market_making_backtest_env {self.id} force_close received")
        self.backtest_launcher.kill()

    def init(self):
        if self.initialized:
            print(rf"WARNING MarketMakingBacktestEnv {self.id}: already initialized")
            return

        # start backtest launcher
        output = self._start_backtest()
        if not output or self.jar_is_killed:
            if self.initial_trial_error >= self.INIT_TRIALS_ERROR:
                print(
                    rf"ERROR MarketMakingBacktestEnv {self.id}: jar is killed in init {self.initial_trial_error}/{self.INIT_TRIALS_ERROR}"
                )
                raise Exception(
                    rf"ERROR MarketMakingBacktestEnv {self.id}: jar is killed in init {self.initial_trial_error} / {self.INIT_TRIALS_ERROR} trials"
                )
            print(
                rf"WARNING MarketMakingBacktestEnv {self.id}: not output:{output} or jar_is_killed: {self.jar_is_killed}  in init {self.initial_trial_error}/{self.INIT_TRIALS_ERROR} -> retrying..."
            )
            self.backtest_launcher.kill()
            self.initial_trial_error += 1
            time.sleep(2)
            return self.init()
        else:
            self.initial_trial_error = 0

        self.initialized = True
        return super().init()

    def step(self, action):
        """
        Get current state of the environment's dynamics.

        Args:
            action (object): an action provided by the agent
        Returns:
            observation (object): agent's observation of the current environment
            reward (float) : amount of reward returned after previous action
            done (boolean): whether the environment wants to end the agent's eposode, in which case further step() calls will return undefined results
            truncated (boolean): whether the step was truncated
            info (dict): contains auxiliary diagnostic information (helpful for debugging, and sometimes learning)
        """
        if self.continuous_action_adaptor is not None:
            action = self.continuous_action_adaptor.adapt_action(action)

        observation, reward, done, truncated, information = super().step(action)
        if done:
            # print information as json in the console
            iterations = information.get('iterations', -1)
            print(
                rf"PYTHON: {self.id} episode done = {done} received after {iterations} steps in this iteration"
            )
            # print(json.dumps(information, indent=4, sort_keys=True))

        if not done and self.jar_is_killed:
            iterations = information.get('iterations', -1)
            print(
                rf"WARNING: {self.id} episode not done = {done} received but jar is killed =>  truncated  after {iterations} steps in this iteration"
            )
            done = True
            truncated = True

        if 'is_success' in information:
            is_true = information['is_success'].lower().strip() == 'true'
            is_one = information['is_success'].lower().strip() == '1'
            information['is_success'] = is_true or is_one

        return observation, reward, done, truncated, information
