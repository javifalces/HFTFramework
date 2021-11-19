import threading
import os
from enum import Enum
import time
import pandas as pd
from backtest.input_configuration import TrainInputConfiguration
from configuration import operative_system

DEFAULT_JVM_WIN = '-Xmx8000M'
DEFAULT_JVM_UNIX = '-Xmx8000M'


class TrainLauncherState(Enum):
    created = 0
    running = 1
    finished = 2


class TrainLauncher(threading.Thread):
    VERBOSE_OUTPUT = False
    if operative_system == 'windows':
        DEFAULT_JVM = DEFAULT_JVM_WIN
    else:
        DEFAULT_JVM = DEFAULT_JVM_UNIX

    def __init__(
            self,
            train_input_configuration: TrainInputConfiguration,
            id: str,
            jar_path='Backtest.jar',
            jvm_options: str = DEFAULT_JVM,
    ):
        threading.Thread.__init__(self)
        self.train_input_configuration = train_input_configuration
        self.id = id
        self.jar_path = jar_path
        self.jvm_options = jvm_options
        self.jvm_options += f' -Dlog.appName=train_model'  # change log name
        self.state = TrainLauncherState.created
        # https://github.com/eclipse/deeplearning4j/issues/2981
        self.task = 'java %s -jar %s' % (self.jvm_options, self.jar_path)

    def run(self):
        self.state = TrainLauncherState.running
        file_content = self.train_input_configuration.get_json()
        # save it into file
        filename = os.getcwd() + os.sep + self.train_input_configuration.get_filename()
        textfile = open(filename, 'w')
        textfile.write(file_content)
        textfile.close()

        command_to_run = self.task + ' %s' % filename
        print('pwd=%s' % os.getcwd())
        if self.VERBOSE_OUTPUT:
            command_to_run += '>%sout.log' % (os.getcwd() + os.sep)
        ret = os.system(command_to_run)
        if ret != 0:
            print("error launching %s" % (command_to_run))

        print('%s finished with code %d' % (self.id, ret))
        # remove input file
        self.state = TrainLauncherState.finished
        if ret==0:
            os.remove(filename)

def clean_javacpp():
    from pathlib import Path
    home = str(Path.home())
    java_cpp = home+os.sep+rf'.javacpp\cache'
    print('cleaning java_cpp: %s' % java_cpp)
    os.remove(java_cpp)

class TrainLauncherController:
    def __init__(self, train_launcher: TrainLauncher):
        self.train_launchers = [train_launcher]
        self.max_simultaneous = 1

    def run(self):
        # clean_javacpp()
        sent = []
        start_time = time.time()
        while 1:
            running = 0
            for train_launcher in self.train_launchers:
                if train_launcher.state == TrainLauncherState.running:
                    running += 1
            if (self.max_simultaneous - running) > 0:
                backtest_waiting = [
                    backtest
                    for backtest in self.train_launchers
                    if backtest not in sent
                ]

                for idx in range(
                        min(self.max_simultaneous - running, len(backtest_waiting))
                ):
                    train_launcher = backtest_waiting[idx]
                    print("launching %s" % train_launcher.id)
                    train_launcher.start()
                    sent.append(train_launcher)

            processed = [t for t in sent if t.state == TrainLauncherState.finished]
            if len(processed) == len(self.train_launchers):
                seconds_elapsed = time.time() - start_time
                print(
                    'finished %d training in %d minutes'
                    % (len(self.train_launchers), seconds_elapsed / 60)
                )
                break
            time.sleep(0.01)


if __name__ == '__main__':
    train_input_configuration = TrainInputConfiguration(memory_path='memoryReplay_sample.csv',
                                                        output_model_path='output_python.model',
                                                        action_columns=6, state_columns=6, number_epochs=200
                                                        )

    train_launcher = TrainLauncher(train_input_configuration=train_input_configuration, id='main_launcher',
                                   jar_path=rf'D:\javif\Coding\cryptotradingdesk\java\executables\Backtest\target\Backtest.jar')
    train_launcher_controller = TrainLauncherController(train_launcher=train_launcher)
    train_launcher_controller.run()
