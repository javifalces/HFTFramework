import os

from enum import Enum
from pathlib import Path
from configuration import (
    ZEROMQ_JAR_PATH,
    operative_system,
    LAMBDA_INPUT_PATH,
    LAMBDA_OUTPUT_PATH,
)
import json

from trading_algorithms.reinforcement_learning.rl_algorithm import (
    BaseModelType,
    ReinforcementLearningActionType,
)


class AlgorithmState(Enum):
    created = 0
    running = 1
    finished = 2


DEFAULT_JVM_WIN = '-Xmx2048M'
DEFAULT_JVM_UNIX = '-Xmx2048M'

import subprocess


class AlgoTradingZeroMqLauncher:
    DETACHED_PROCESS = 0x00000008
    VERBOSE_OUTPUT = False
    base_output = LAMBDA_OUTPUT_PATH + os.sep
    if operative_system == 'windows':
        DEFAULT_JVM = DEFAULT_JVM_WIN
        PREFIX_START = ""
    else:
        DEFAULT_JVM = DEFAULT_JVM_UNIX

    def __init__(
            self,
            algorithm_settings_path: str,
            jar_path=ZEROMQ_JAR_PATH,
            jvm_options: str = DEFAULT_JVM,
    ) -> None:
        if not os.path.isfile(algorithm_settings_path):
            print(f"algorithm_settings_path not found {algorithm_settings_path}")
            raise FileNotFoundError(
                f"algorithm_settings_path not found {algorithm_settings_path}"
            )
        self.algorithm_settings_path = algorithm_settings_path

        self.jar_path = jar_path
        self.class_path_folder = Path(self.jar_path).parent
        self.state = AlgorithmState.created
        self.jvm_options = '-Duser.timezone=GMT ' + jvm_options
        self.algorithm_name = self._read_algorithm_name()

        (
            algorithm_name,
            rl_host,
            rl_port,
            base_model,
            reinforcement_learning_action_type,
        ) = self._read_rl_gym_configuration()
        if rl_port > 0:
            print(
                rf" start python server gym_agent_launcher -> algorithm_name={algorithm_name} rl_host={rl_host} rl_port={rl_port} base_model={base_model}"
            )
            self._start_gym_agent_launcher(
                algorithm_name,
                rl_host,
                rl_port,
                base_model,
                reinforcement_learning_action_type,
            )

        self.output_path = None
        if self.algorithm_name is not None:
            self.output_path = rf"{self.base_output}"
            # copy models
            self.jvm_options += f' -Doutput.path={self.output_path}'  # change log name
            self.jvm_options += (
                f' -Dlog.appName={self.algorithm_name}'  # change log name
            )

        # jvm_options += f' -Dlog.appName={algo_name}'  # change log name
        self.pid = None
        self.process = None

        self.gym_agent_pid = None
        self.gym_agent_process = None

    def _start_gym_agent_launcher(
            self,
            algorithm_name: str,
            rl_host: str,
            rl_port: int,
            base_model: str,
            reinforcement_learning_action_type: str,
    ):
        from trading_algorithms.reinforcement_learning.rl_algorithm import RLAlgorithm

        agent_model_path = RLAlgorithm.get_agent_model_path(algorithm_name)
        normalizer_model_path = RLAlgorithm.get_normalizer_model_path(algorithm_name)
        action_adapter_path = RLAlgorithm.get_action_adaptor_path(algorithm_name)

        if not os.path.isfile(agent_model_path):
            raise Exception(f"agent_model_path not found {agent_model_path}")
        import subprocess

        python_executable = 'gym_agent_launcher.py'
        task = rf'python {python_executable}'

        args = rf"{rl_host} {rl_port} {agent_model_path} {normalizer_model_path} {base_model} {reinforcement_learning_action_type} {action_adapter_path}"  # <rl_host> <rl_port> <model_path> <normalizer_model_path> <base_model> <reinforcement_learning_action_type> <action_adapter_path>
        command_to_run = task + rf' {args}'
        print('pwd=%s' % os.getcwd())
        if self.VERBOSE_OUTPUT:
            command_to_run += '>%sout.log' % (os.getcwd() + os.sep)

        self.gym_agent_process = subprocess.Popen(
            command_to_run, creationflags=subprocess.CREATE_NEW_CONSOLE
        )

        self.gym_agent_pid = self.gym_agent_process.pid

    def _read_algorithm_name(self):
        try:
            with open(self.algorithm_settings_path, 'r') as myfile:
                data = myfile.read()
            settings = json.loads(data)
            return settings["algorithm"]["algorithmName"]
        except Exception as e:
            print(f"not the right json format on {self.algorithm_settings_path}")
        return None

    def _read_rl_gym_configuration(self) -> (str, str, int, str, str):
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
        )

        try:
            with open(self.algorithm_settings_path, 'r') as myfile:
                data = myfile.read()
            settings = json.loads(data)
            parameters = settings["algorithm"]["parameters"]
            algorithm_name = settings["algorithm"]["algorithmName"]
            rl_host = parameters.get(RlAlgorithmParameters.rl_host, "")
            rl_port = parameters.get(RlAlgorithmParameters.rl_port, -1)
            base_model = parameters.get(RlAlgorithmParameters.model, BaseModelType.PPO)
            reinforcement_learning_action_type = parameters.get(
                RlAlgorithmParameters.action_type,
                ReinforcementLearningActionType.continuous,
            )
            return algorithm_name, rl_host, rl_port, base_model
        except Exception as e:
            print(f"not the right json format on {self.algorithm_settings_path}")
        return "", "", -1, ""

    def run(self):
        if (
                self.state == AlgorithmState.created
                or self.state == AlgorithmState.finished
        ):
            self.task = 'java %s -jar %s' % (self.jvm_options, self.jar_path)
            self.state = AlgorithmState.running

            command_to_run = self.task + ' %s' % self.algorithm_settings_path
            print('pwd=%s' % os.getcwd())
            if self.VERBOSE_OUTPUT:
                command_to_run += '>%sout.log' % (os.getcwd() + os.sep)

            self.process = subprocess.Popen(
                command_to_run, creationflags=subprocess.CREATE_NEW_CONSOLE
            )

            self.pid = self.process.pid
            print(f"started process {self.pid} ")
        else:
            print(f"we are in state {self.state}-> cant run")
        return self.process

    def kill(self):
        if self.state == AlgorithmState.running and self.process is not None:
            print(f"killing process {self.pid}")
            if self.process.poll() is None:
                self.process.kill()
                self.state = AlgorithmState.finished


import glob


class AutomaticStartZeroMqTrading:
    def __init__(self, filter_regexp: str = '*.json', vm_options: str = None):
        self.filter_regexp = filter_regexp
        self.algo_trading_processes = []
        self.vm_options = vm_options

    def start(self):
        path_with_regexp = LAMBDA_INPUT_PATH + os.sep + self.filter_regexp
        configuration_files_filtered = glob.glob(path_with_regexp, recursive=True)
        if len(configuration_files_filtered) == 0:
            print(
                f"no config files found for {self.filter_regexp} in {LAMBDA_INPUT_PATH}"
            )
            return
        else:
            print(
                f"AutomaticStartZeroMqTrading going to launch {len(configuration_files_filtered)} processes"
            )
            for configuration_file in configuration_files_filtered:
                if self.vm_options is None:
                    launcher = AlgoTradingZeroMqLauncher(
                        algorithm_settings_path=configuration_file
                    )
                else:
                    launcher = AlgoTradingZeroMqLauncher(
                        algorithm_settings_path=configuration_file,
                        jvm_options=self.vm_options,
                    )
                self.algo_trading_processes.append(launcher)
                launcher.run()

    def stop(self):
        print(
            f"AutomaticStartZeroMqTrading going to kill {len(self.algo_trading_processes)} processes"
        )
        for algo_trading_proces in self.algo_trading_processes:
            algo_trading_proces.kill()


if __name__ == '__main__':
    import sys

    # launcher = AlgoTradingZeroMqLauncher(
    #     algorithm_settings_path=LAMBDA_INPUT_PATH + os.sep + 'parameters_rsi_dqn_eurusd.json')
    # launcher.run()
    # import time
    # time.sleep(50)
    # launcher.kill()

    if len(sys.argv) > 1:
        vm_options = sys.argv[1]

    else:
        vm_options = DEFAULT_JVM_WIN
    print(f"vm_options={vm_options}")
    automatic_launcher = AutomaticStartZeroMqTrading(
        filter_regexp='*.json', vm_options=vm_options
    )
    try:
        automatic_launcher.start()
        print("Ctrl+C to kill all algos")
    except KeyboardInterrupt:
        automatic_launcher.stop()
        # quit
        sys.exit()
