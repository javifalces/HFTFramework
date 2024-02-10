import pathlib
import os
import time

import platform


operative_system = platform.system().lower()  # windows or unix?

PROJECT_PATH = pathlib.Path(__file__).parent.absolute()

LAMBDA_OUTPUT_PATH = os.getenv(
    key='LAMBDA_OUTPUT_PATH', default=str(PROJECT_PATH) + os.sep + 'output'
)

LAMBDA_INPUT_PATH = os.getenv(
    key='LAMBDA_INPUT_PATH', default=str(PROJECT_PATH) + os.sep + 'input'
)

LAMBDA_TEMP_PATH = os.getenv(
    key='LAMBDA_TEMP_PATH', default=str(PROJECT_PATH) + os.sep + 'temp'
)  # must be the same as application.properties

DEGIRO_USER = os.getenv(key='DEGIRO_USER', default="TEST")

DEGIRO_PASSWORD = os.getenv(key='DEGIRO_PASSWORD', default="TEST")

SHARPE_BACKTEST_FREQ = (
    '5S'  # equity curve group by this and last , returns and group by this and sum
)

# USE_IPC_RL_TRAINING
USE_IPC_RL_TRAINING = os.getenv(key='USE_IPC_RL_TRAINING', default=False)

def get_reinforcement_learning_framework():
    from trading_algorithms.reinforcement_learning.core.core_rl_algorithm import (
        CoreRlAlgorithmEnum,
    )

    return CoreRlAlgorithmEnum.baselines3


def get_lambda_input():
    return os.getenv(
        key='LAMBDA_INPUT_PATH', default=str(PROJECT_PATH) + os.sep + 'input'
    )


default_jar_path = (
        str(pathlib.Path(PROJECT_PATH).parent)
        + os.sep
        + os.path.join('java', 'executables', 'Backtest', 'target', 'Backtest.jar')
)
BACKTEST_JAR_PATH = os.getenv(key='LAMBDA_JAR_PATH', default=default_jar_path)

default_zero_jar_path = (
        str(pathlib.Path(PROJECT_PATH).parent)
        + os.sep
        + os.path.join(
    'java', 'executables', 'AlgoTradingZeroMq', 'target', 'AlgoTradingZeroMq.jar'
)
)
ZEROMQ_JAR_PATH = os.getenv(key='LAMBDA_ZEROMQ_JAR_PATH', default=default_zero_jar_path)
import datetime

today_folder = datetime.datetime.today().strftime('%y%m%d')
log_path = (
        os.getenv("LAMBDA_LOGS_PATH", str(PROJECT_PATH) + os.sep + "logs")
        + os.sep
        + today_folder
)
pathlib.Path(log_path).mkdir(parents=True, exist_ok=True)

if not os.path.exists(log_path):
    os.mkdir(path=log_path)

if not os.path.exists(LAMBDA_OUTPUT_PATH):
    os.mkdir(path=LAMBDA_OUTPUT_PATH)
if not os.path.exists(LAMBDA_TEMP_PATH):
    os.mkdir(path=LAMBDA_TEMP_PATH)

LAMBDA_DATA_PATH = os.getenv("LAMBDA_DATA_PATH", 'X:\\')
# %%
global initialized_configuration
we_are_initialized = (
        "initialized_configuration" in locals()
        or "initialized_configuration" in globals()
        or os.getenv("LAMBDA_DATA_INITIALIZED") == "true"
)
if not we_are_initialized:
    os.environ['LAMBDA_DATA_INITIALIZED'] = "true"
    initialized_configuration = True
    print("PROJECT_PATH=%s" % PROJECT_PATH)
    print("LAMBDA_OUTPUT_PATH=%s" % LAMBDA_OUTPUT_PATH)
    print("LAMBDA_TEMP_PATH=%s" % LAMBDA_TEMP_PATH)
    print("BACKTEST_JAR_PATH(LAMBDA_JAR_PATH)=%s" % BACKTEST_JAR_PATH)
    print("log_path(LAMBDA_LOGS_PATH)=%s" % log_path)
    print("LAMBDA_DATA_PATH=%s" % LAMBDA_DATA_PATH)

    def create_application_properties():
        dict_write = {}
        dict_write['temp.path'] = LAMBDA_TEMP_PATH
        dict_write['output.path'] = LAMBDA_OUTPUT_PATH
        dict_write['parquet.path'] = LAMBDA_DATA_PATH

        output = ''
        for key in dict_write.keys():
            output += (
                    '%s:%s' % (key, dict_write[key].replace(os.sep, os.sep + os.sep))
                    + os.linesep
            )
        output = output[: -len(os.linesep)]

        with open("application.properties", "w") as text_file:
            text_file.write(output)

    create_application_properties()
# %% LOGGER
import os
import pathlib
import logging
import datetime

loggers = {}


def is_jupyter_notebook():
    import __main__ as main

    return not hasattr(main, '__file__')


def clean_javacpp():
    from pathlib import Path

    home = str(Path.home())
    java_cpp = home + os.sep + rf'.javacpp\cache'
    print('cleaning java_cpp: %s' % java_cpp)
    os.remove(java_cpp)


def clean_gpu_memory():
    # print(rf"cleaning gpu memory")
    from numba import cuda

    device = cuda.get_current_device()
    device.reset()


def get_logger(framework_name="python_lambda", print_on_console: bool = False):
    if framework_name in loggers.keys():
        return loggers[framework_name]

    level = logging.DEBUG
    formatLog = " %(asctime)s -[%(filename)s:%(lineno)s - %(funcName)20s() ] %(levelname)s - %(message)s"

    logger = logging.getLogger(framework_name)
    logger.setLevel(level)

    # create a file handler
    log_name = "%s_%s.log" % (
        framework_name,
        datetime.datetime.today().strftime("%Y%m%d"),
    )
    log_complete_path = log_path + os.sep + log_name
    handler = logging.FileHandler(log_complete_path)
    handler.setLevel(logging.DEBUG)

    # create a logging format
    formatter = logging.Formatter(formatLog)
    logging.Formatter.converter = time.gmtime  # logger in UTC

    handler.setFormatter(formatter)
    # To print on screen
    if print_on_console:
        import sys

        ch = logging.StreamHandler(sys.stdout)
        ch.setLevel(logging.INFO)
        ch.setFormatter(formatter)
        logger.addHandler(ch)
    # add the handlers to the logger
    logger.addHandler(handler)
    loggers[framework_name] = logger
    return logger
