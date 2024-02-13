import signal
import subprocess
from enum import Enum
import glob

from trading_algorithms.algorithm_enum import AlgorithmEnum
from backtest.input_configuration import (
    InputConfiguration,
    BacktestConfiguration,
    AlgorithmConfiguration,
)
import os
import threading
import time
from pathlib import Path
import pandas as pd
from configuration import LAMBDA_OUTPUT_PATH, operative_system, is_jupyter_notebook


class BacktestState(Enum):
    created = 0
    running = 1
    finished = 2


OUTPUT_PATH = LAMBDA_OUTPUT_PATH
REMOVE_FINAL_CSV = True
REMOVE_INPUT_JSON = True

DEFAULT_JVM_WIN = '-Xmx6G'
DEFAULT_JVM_UNIX = '-Xmx6G'
TIMEOUT_SECONDS = 60 * 10  # 10 minutes


class ListenerKill:
    def notify_kill(self, code):
        pass


class TimeoutException(Exception):
    def __init__(self, *args: object) -> None:
        super().__init__(*args)


class BacktestLauncher(threading.Thread):
    VERBOSE_OUTPUT = False
    if operative_system == 'windows':
        DEFAULT_JVM = DEFAULT_JVM_WIN
    else:
        DEFAULT_JVM = DEFAULT_JVM_UNIX

    def __init__(
            self,
            input_configuration: InputConfiguration,
            id: str,
            jar_path='Backtest.jar',
            jvm_options: str = DEFAULT_JVM,
    ):
        self.pid = None
        self.proc = None

        threading.Thread.__init__(self)
        self.input_configuration = input_configuration
        self.jar_path = jar_path
        self.output_path = OUTPUT_PATH
        self.class_path_folder = Path(self.jar_path).parent

        self.algorithm_name = (
            self.input_configuration.algorithm_configuration.algorithm_name
        )
        jvm_options += f' -Dlog.appName={self.algorithm_name}'  # change log name
        jvm_options += (
            f' -Duser.timezone=GMT'  # GMT for printing logs and backtest configuration
        )

        self.task = 'java %s -jar %s' % (jvm_options, self.jar_path)
        self.state = BacktestState.created
        self.id = id
        self.listeners_kill = []
        # if self.id != self.input_configuration.algorithm_configuration.algorithm_name:
        #     self.input_configuration.algorithm_configuration.algorithm_name = self.id
        #     # self.input_configuration.algorithm_configuration.algorithm_name += (
        #     #         '_' + str(id)
        #     # )

        if not os.path.isdir(self.output_path):
            print("mkdir %s" % self.output_path)
            os.mkdir(self.output_path)

    def register_kill_listener(self, listener: ListenerKill):
        self.listeners_kill.append(listener)

    def notify_kill_listeners(self, code: int):
        for listener in self.listeners_kill:
            listener.notify_kill(code)

    def _launch_process_os(self, command_to_run: str):
        '''
        java process is not printing in notebook
        Parameters
        ----------
        command_to_run

        Returns
        -------

        '''
        self.pid = None
        ret = os.system(command_to_run)
        return ret

    def _wait_subproccess(self):
        if not is_jupyter_notebook():
            ret = self.proc.wait(timeout=TIMEOUT_SECONDS)
            return ret
        else:
            start_time = time.time()
            while self.proc.poll() is None:
                elapsed_seconds = time.time() - start_time
                if elapsed_seconds > TIMEOUT_SECONDS:
                    raise TimeoutException(
                        "timeout %d seconds expired subprocess pid:%d"
                        % (TIMEOUT_SECONDS, self.proc.pid)
                    )
                text = self.proc.stdout.read1().decode("utf-8")

                print(text, end='', flush=True)

            ret = self.proc.returncode
            return ret

    def _launch_process_subprocess(self, command_to_run: str):
        import subprocess

        # https://stackoverflow.com/questions/56138384/capture-jupyter-notebook-stdout-with-subprocess
        if not is_jupyter_notebook():
            stderr_option = None
            stdout_option = None
        else:
            stderr_option = subprocess.PIPE
            stdout_option = subprocess.PIPE

        self.proc = subprocess.Popen(
            command_to_run, stderr=stderr_option, stdout=stdout_option
        )  # <-- redirect stderr to stdout

        self.pid = (
            self.proc.pid
        )  # <--- access `pid` attribute to get the pid of the child process.
        try:
            ret = self._wait_subproccess()
        except (subprocess.TimeoutExpired, TimeoutException):
            print(
                rf"timeout java {TIMEOUT_SECONDS} seconds expired -> kill the process pid:{self.proc.pid}"
            )
            self.proc.kill()
            ret = -1

        return ret

    def run(self):
        self.state = BacktestState.running
        file_content = self.input_configuration.get_json()

        # save it into file
        filename = os.getcwd() + os.sep + self.input_configuration.get_filename()
        textfile = open(filename, 'w')
        textfile.write(file_content)
        textfile.close()

        command_to_run = self.task + ' %s 1' % filename
        print('pwd=%s' % os.getcwd())
        if self.VERBOSE_OUTPUT:
            command_to_run += '>%sout.log' % (os.getcwd() + os.sep)

        ret = self._launch_process_subprocess(command_to_run)
        # ret = self._launch_process_os(command_to_run)
        if ret != 0:
            print("error launching %s" % (command_to_run))

        print('%s %s finished with code %d' % (self.id, self.algorithm_name, ret))
        self.notify_kill_listeners(ret)
        self.state = BacktestState.finished
        # remove input file
        if REMOVE_INPUT_JSON and os.path.exists(filename):
            os.remove(filename)

        self.proc = None
        self.pid = None

    def kill(self):
        import traceback

        try:
            if self.proc is not None:
                print(rf"WARNING: kill the process pid:{self.proc.pid}")
                # traceback.print_stack()
                self.proc.kill()
                return

            if self.pid is not None:
                print(rf"WARNING: kill the os.pid:{self.pid}")
                # traceback.print_stack()
                os.kill(self.pid, signal.SIGTERM)
        except Exception as e:
            print(f"kill error:{e}")

    def __del__(self):
        self.kill()


class BacktestLauncherController:
    def __init__(self, backtest_launchers: list, max_simultaneous: int = 4):
        self.backtest_launchers = backtest_launchers
        self.max_simultaneous = max_simultaneous
        self.last_output = {}

    def _initial_clean(self, backtest_launcher):
        input_configuration = backtest_launcher.input_configuration
        algo_name = input_configuration.algorithm_configuration.algorithm_name

        csv_filenames = glob.glob(
            backtest_launcher.output_path + os.sep + 'trades_table_%s_*.csv' % algo_name
        )
        for csv_filename in csv_filenames:
            os.remove(csv_filename)

    def execute_lambda(self):
        sent = []
        start_time = time.time()
        while 1:
            running = 0
            for backtest_launcher in self.backtest_launchers:
                if backtest_launcher.state == BacktestState.running:
                    running += 1
            if (self.max_simultaneous - running) > 0:
                backtest_waiting = [
                    backtest
                    for backtest in self.backtest_launchers
                    if backtest not in sent
                ]

                for idx in range(
                        min(self.max_simultaneous - running, len(backtest_waiting))
                ):
                    backtest_launcher = backtest_waiting[idx]
                    print("launching %s" % backtest_launcher.id)
                    self._initial_clean(backtest_launcher=backtest_launcher)
                    backtest_launcher.start()
                    sent.append(backtest_launcher)

            processed = [t for t in sent if t.state == BacktestState.finished]
            if len(processed) == len(self.backtest_launchers):
                seconds_elapsed = time.time() - start_time
                print(
                    'finished %d backtests in %d minutes'
                    % (len(self.backtest_launchers), seconds_elapsed / 60)
                )
                break
            time.sleep(0.01)

    def execute_joblib(self):
        from utils.paralellization_util import process_jobs_joblib

        jobs = []
        for backtest_launcher in self.backtest_launchers:
            job = {"func": backtest_launcher.run}
            jobs.append(job)
        process_jobs_joblib(jobs=jobs, num_threads=self.max_simultaneous)

    def _get_start_arb_trades_df(self, backtest_launcher, algo_name: str, path: list):
        ## get rest of instruments and combine
        csv_filenames = glob.glob(
            backtest_launcher.output_path + os.sep + 'trades_table_%s_*.csv' % algo_name
        )
        for csv_filename in csv_filenames:
            try:
                df_temp = pd.read_csv(csv_filename)
                instrument_pk_list = csv_filename.split(os.sep)[-1].split('_')[-2:]
                instrument_pk = '_'.join(instrument_pk_list).split('.')[0]
                df_temp['instrument'] = instrument_pk
                df_temp['historicalUnrealizedPnl'] = (
                    df_temp['historicalUnrealizedPnl'].diff().fillna(0.0)
                )
                df_temp['historicalTotalPnl'] = (
                    df_temp['historicalTotalPnl'].diff().fillna(0.0)
                )
                df_temp['historicalRealizedPnl'] = (
                    df_temp['historicalRealizedPnl'].diff().fillna(0.0)
                )
                if df is None:
                    df = df_temp
                else:
                    df = df.append(df_temp)
                path.append(csv_filename)
            except Exception as e:
                print(
                    'something goes wrong reading output csv %s : %s'
                    % (csv_filename, str(e))
                )
        if df is not None:
            df = df.set_index(keys='date').sort_index(ascending=True).reset_index()
            df['historicalUnrealizedPnl'] = df['historicalUnrealizedPnl'].cumsum()
            df['historicalRealizedPnl'] = df['historicalRealizedPnl'].cumsum()
            df['historicalTotalPnl'] = (
                    df['historicalUnrealizedPnl'] + df['historicalRealizedPnl']
            )
            print(
                f'{algo_name} finished with {len(df)} trades on {len(path)} csv files'
            )
        else:
            print(f'{algo_name} finished with empty trades on {len(path)} csv files')

        return df, path

    def run(self) -> dict:

        # self.execute_lambda()
        self.execute_joblib()
        # get output dataframes
        output = {}
        for idx, backtest_launcher in enumerate(self.backtest_launchers):
            path = []
            df = None
            input_configuration = backtest_launcher.input_configuration
            algo_name = input_configuration.algorithm_configuration.algorithm_name
            output[backtest_launcher.id] = None
            instrument_pk = input_configuration.backtest_configuration.instrument_pk
            # if algo_name.startswith(AlgorithmEnum.stat_arb):
            #     df, path = self._get_start_arb_trades_df(
            #         backtest_launcher, algo_name, path
            #     )
            # else:
            csv_filename = 'trades_table_%s_%s.csv' % (algo_name, instrument_pk)
            path = backtest_launcher.output_path + os.sep + csv_filename
            if not os.path.exists(path):
                print('%s output file %s not exist' % (backtest_launcher.id, path))
                continue
            else:
                df = pd.read_csv(path)

            if df['date'].iloc[0].startswith("1970"):
                df = df.iloc[1:]

            output[backtest_launcher.id] = df
            if df is None:
                print('%s with None trades' % (algo_name))
            else:
                print(
                    f'{algo_name} with {len(df)} trades [{idx}/{len(self.backtest_launchers) - 1}]'
                )
            if REMOVE_FINAL_CSV:
                try:
                    if isinstance(path, str) and os.path.exists(path):
                        os.remove(path)
                    if isinstance(path, list):
                        for path_i in path:
                            if os.path.exists(path_i):
                                os.remove(path_i)
                except Exception as e:
                    print('error removing csv %s : %s' % (path, str(e)))

            ##remove position json
            position_files = glob.glob(
                backtest_launcher.output_path
                + os.sep
                + '%s_paperTradingEngine_position.json' % (algo_name)
            )
            try:
                for position_file in position_files:
                    if os.path.exists(position_file):
                        os.remove(position_file)
            except Exception as e:
                print(
                    'error removing position json %s : %s'
                    % (backtest_launcher.output_path, str(e))
                )

        self.last_output = output
        print(rf"Finished backtest with {len(self.backtest_launchers)} launchers")
        return output


if __name__ == '__main__':
    import datetime

    backtest_configuration = BacktestConfiguration(
        start_date=datetime.datetime(year=2020, day=8, month=12),
        end_date=datetime.datetime(year=2020, day=8, month=12),
        instrument_pk='btcusdt_binance',
    )

    parameters = {
        "risk_aversion": "0.9",
        "position_multiplier": "100",
        "window_tick": "100",
        "minutes_change_k": "10",
        "quantity": "0.0001",
        "k_default": "0.00769",
        "spread_multiplier": "5.0",
        "first_hour": "7",
        "last_hour": "19",
    }

    algorith_configuration = AlgorithmConfiguration(
        algorithm_name='AvellanedaStoikov', parameters=parameters
    )

    parameters_2 = {
        "risk_aversion": "0.2",
        "position_multiplier": "100",
        "window_tick": "100",
        "minutes_change_k": "10",
        "quantity": "0.0001",
        "k_default": "0.00769",
        "spread_multiplier": "5.0",
        "first_hour": "7",
        "last_hour": "19",
    }

    algorith_configuration_2 = AlgorithmConfiguration(
        algorithm_name='AvellanedaStoikov', parameters=parameters_2
    )

    input_configuration = InputConfiguration(
        backtest_configuration=backtest_configuration,
        algorithm_configuration=algorith_configuration,
    )
    backtest_launcher_1 = BacktestLauncher(
        input_configuration=input_configuration,
        id='main_test_as',
        jar_path=rf'D:\javif\Coding\cryptotradingdesk\java\executables\Backtest\target\Backtest.jar',
    )

    input_configuration_2 = InputConfiguration(
        backtest_configuration=backtest_configuration,
        algorithm_configuration=algorith_configuration_2,
    )

    backtest_launcher_2 = BacktestLauncher(
        input_configuration=input_configuration_2,
        id='main_test_as_2',
        jar_path=rf'D:\javif\Coding\cryptotradingdesk\java\executables\Backtest\target\Backtest.jar',
    )

    backtest_launchers = [backtest_launcher_1, backtest_launcher_2]
    # train_launchers = [backtest_launcher_1]
    backtest_controller = BacktestLauncherController(
        backtest_launchers=backtest_launchers
    )

    backtest_controller.run()
