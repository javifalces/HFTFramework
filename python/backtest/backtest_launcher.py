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
from configuration import BACKTEST_OUTPUT_PATH, operative_system


class BacktestState(Enum):
    created = 0
    running = 1
    finished = 2


OUTPUT_PATH = BACKTEST_OUTPUT_PATH
REMOVE_FINAL_CSV = True
REMOVE_INPUT_JSON = True

DEFAULT_JVM_WIN = '-Xmx24000M'
DEFAULT_JVM_UNIX = '-Xmx8000M'


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
        threading.Thread.__init__(self)
        self.input_configuration = input_configuration
        self.jar_path = jar_path
        self.output_path = OUTPUT_PATH
        self.class_path_folder = Path(self.jar_path).parent

        # https://github.com/eclipse/deeplearning4j/issues/2981
        algo_name = self.input_configuration.algorithm_configuration.algorithm_name
        jvm_options += f' -Dlog.appName={algo_name}'  # change log name
        self.task = 'java %s -jar %s' % (jvm_options, self.jar_path)
        self.state = BacktestState.created
        self.id = id

        if self.id != self.input_configuration.algorithm_configuration.algorithm_name:
            self.input_configuration.algorithm_configuration.algorithm_name += (
                    '_' + str(id)
            )

        if not os.path.isdir(self.output_path):
            print("mkdir %s" % self.output_path)
            os.mkdir(self.output_path)

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

        ret = os.system(command_to_run)
        if ret != 0:
            print("error launching %s" % (command_to_run))

        print('%s finished with code %d' % (self.id, ret))
        self.state = BacktestState.finished
        # remove input file
        if REMOVE_INPUT_JSON and os.path.exists(filename):
            os.remove(filename)


class BacktestLauncherController:
    def __init__(self, backtest_launchers: list, max_simultaneous: int = 4):
        self.backtest_launchers = backtest_launchers
        self.max_simultaneous = max_simultaneous

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

    def run(self) -> dict:

        # self.execute_lambda()
        self.execute_joblib()
        # get output dataframes
        output = {}
        for backtest_launcher in self.backtest_launchers:
            path = []
            df = None
            input_configuration = backtest_launcher.input_configuration
            algo_name = input_configuration.algorithm_configuration.algorithm_name
            output[backtest_launcher.id] = None
            instrument_pk = input_configuration.backtest_configuration.instrument_pk
            if algo_name.startswith(AlgorithmEnum.stat_arb):
                ## get rest of instruments and combine
                csv_filenames = glob.glob(
                    backtest_launcher.output_path
                    + os.sep
                    + 'trades_table_%s_*.csv' % algo_name
                )
                for csv_filename in csv_filenames:
                    try:
                        df_temp = pd.read_csv(csv_filename)
                        instrument_pk_list = csv_filename.split(os.sep)[-1].split('_')[
                                             -2:
                                             ]
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
                    df = (
                        df.set_index(keys='date')
                        .sort_index(ascending=True)
                        .reset_index()
                    )
                    df['historicalUnrealizedPnl'] = df[
                        'historicalUnrealizedPnl'
                    ].cumsum()
                    df['historicalRealizedPnl'] = df['historicalRealizedPnl'].cumsum()
                    df['historicalTotalPnl'] = (
                            df['historicalUnrealizedPnl'] + df['historicalRealizedPnl']
                    )
                    print(
                        f'{algo_name} finished with {len(df)} trades on {len(path)} csv files'
                    )
                else:
                    print(
                        f'{algo_name} finished with empty trades on {len(path)} csv files'
                    )

            else:
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
                print('%s with %d trades' % (algo_name, len(df)))
            if REMOVE_FINAL_CSV:
                if isinstance(path, str):
                    os.remove(path)
                if isinstance(path, list):
                    for path_i in path:
                        os.remove(path_i)
            ##remove position json
            position_files = glob.glob(
                backtest_launcher.output_path
                + os.sep
                + '%s_paperTradingEngine_position.json' % (algo_name)
            )
            for position_file in position_files:
                os.remove(position_file)

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
