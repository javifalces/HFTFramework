import pathlib
import os


import platform

operative_system = platform.system().lower()  # windows or unix?

PROJECT_PATH = pathlib.Path(__file__).parent.absolute()

BACKTEST_OUTPUT_PATH = os.getenv(
    key='LAMBDA_OUTPUT_PATH', default=str(PROJECT_PATH) + os.sep + 'output'
)  # must be the same as application.properties
LAMBDA_OUTPUT_PATH = os.getenv(
    key='LAMBDA_OUTPUT_PATH', default=str(PROJECT_PATH) + os.sep + 'output'
)
LAMBDA_INPUT_PATH = os.getenv(
    key='LAMBDA_INPUT_PATH', default=str(PROJECT_PATH) + os.sep + 'input'
)

BACKTEST_TEMP_PATH = os.getenv(
    key='LAMBDA_TEMP_PATH', default=str(PROJECT_PATH) + os.sep + 'temp'
)  # must be the same as application.properties
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

log_path = os.getenv("LAMBDA_LOGS_PATH", str(PROJECT_PATH) + os.sep + "logs")
pathlib.Path(log_path).mkdir(parents=True, exist_ok=True)


if not os.path.exists(log_path):
    os.mkdir(path=log_path)

if not os.path.exists(BACKTEST_OUTPUT_PATH):
    os.mkdir(path=BACKTEST_OUTPUT_PATH)
if not os.path.exists(BACKTEST_TEMP_PATH):
    os.mkdir(path=BACKTEST_TEMP_PATH)

PARQUET_PATH_DB = os.getenv("LAMBDA_PARQUET_TICK_DB", 'X:\\')
#%%
if not "initiliazed_configuration" in locals():
    initiliazed_configuration = True
    print("PROJECT_PATH=%s" % PROJECT_PATH)
    print("BACKTEST_OUTPUT_PATH(LAMBDA_OUTPUT_PATH)=%s" % BACKTEST_OUTPUT_PATH)
    print("BACKTEST_TEMP_PATH(LAMBDA_TEMP_PATH)=%s" % BACKTEST_TEMP_PATH)
    print("BACKTEST_JAR_PATH(LAMBDA_JAR_PATH)=%s" % BACKTEST_JAR_PATH)
    print("log_path(LAMBDA_LOGS_PATH)=%s" % log_path)
    print("PARQUET_PATH_DB(LAMBDA_PARQUET_TICK_DB)=%s" % PARQUET_PATH_DB)

    def create_application_properties():
        dict_write = {}
        dict_write['temp.path'] = BACKTEST_TEMP_PATH
        dict_write['output.path'] = BACKTEST_OUTPUT_PATH
        dict_write['parquet.path'] = PARQUET_PATH_DB

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
#%% LOGGER
import os
import pathlib
import logging
import datetime


framework_name = "python_lambda"
level = logging.DEBUG
formatLog = " %(asctime)s -[%(filename)s:%(lineno)s - %(funcName)20s() ] %(levelname)s - %(message)s"


logger = logging.getLogger(framework_name)
logger.setLevel(level)

# create a file handler
log_name = "%s_%s.log" % (framework_name, datetime.datetime.today().strftime("%Y%m%d"))
handler = logging.FileHandler(log_path + os.sep + log_name)
handler.setLevel(logging.DEBUG)

# create a logging format
formatter = logging.Formatter(formatLog)
handler.setFormatter(formatter)
# To print on screen
# import sys
#
# ch = logging.StreamHandler(sys.stdout)
# ch.setLevel(logging.DEBUG)
# ch.setFormatter(formatter)
# # add the handlers to the logger
# logger.addHandler(handler)
# logger.addHandler(ch)
