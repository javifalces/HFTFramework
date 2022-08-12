import datetime
from typing import Type
import copy
from trading_algorithms.candles_type import CandleType
from utils.date_utils import date_to_string
from utils.pandas_utils.dataframe_utils import join_by_row

from trading_algorithms.algorithm import Algorithm
from trading_algorithms.algorithm_enum import AlgorithmEnum
from trading_algorithms.dqn_algorithm import StateType
from trading_algorithms.state_utils import (
    StateUtils,
    DEFAULT_MARKET_HORIZON_SAVE,
    DEFAULT_TA_INDICATORS_PERIODS,
    DEFAULT_TA_INDICATORS_PERIODS_BINARY,
)

from backtest.parameter_tuning.ga_configuration import GAConfiguration
from configuration import LAMBDA_OUTPUT_PATH
from backtest.pnl_utils import *

'''
aggressive-> market orders
passive -> mid price orders
level -> use levelTo Quotes
'''
DEFAULT_PARAMETERS = {
    "buyModelType": "",
    "sellModelType": "",
    "modelsFolder": rf"E:\javif\Coding\Python\market_making_fw\java\output\crypto_trend_predictor",
    "maxTimeWaitingActionResultsMs": 1000,  # max time waiting if send action and no result
    "maxTimeWaitingExitMs": 300000,  # max time waiting exit function
    # default default
    "quantity": (0.1),
    "first_hour": (0),
    "last_hour": (24),
    "binaryStateOutputs": 0,  # if 1 states will be 1 or 0 only
    "numberDecimalsState": 2,
    "volumeCandles": 1.0,
    "horizonTicksMarketState": 15.0,
    "otherInstrumentsStates": [],
    "otherInstrumentsMsPeriods": [],
    "marketTickMs": 10,
    "dumpData": 1,
    "dumpSeconds": 30,
    "periodsTAStates": [
        3.0,
        9.0,
        13.0,
        15.0,
        20.0,
        25.0,
        27.0,
        30.0,
        38.0,
        44.0,
        49.0,
        55.0,
        60.0,
        65.0,
        70.0,
        75.0,
        80.0,
        85.0,
        90.0,
    ],
    "dumpFilename": "null",
    "candleTypeBusiness": CandleType.volume_threshold_depth,
    "secondsCandles": 56,
}


# MARKET_COLUMNS = 10
# CANDLE_COLUMNS = 4#if relative is 3 else is 4
# CANDLE_INDICATOR_COLUMNS = 4


class TrendPredictor(Algorithm):
    NAME = AlgorithmEnum.trend_predictor

    def __init__(self, algorithm_info: str, parameters: dict = DEFAULT_PARAMETERS):
        super().__init__(
            algorithm_info=self.NAME + "_" + algorithm_info, parameters=parameters
        )
        self.state_type = StateType.ta_state  # use technical indicators

    # get states
    def _get_default_state_columns(self):
        if self.state_type == StateType.market_state:
            return self._get_market_state_columns()
        if self.state_type == StateType.ta_state:
            return self._get_ta_state_columns()

    def _get_market_state_columns(self):
        private_horizon_ticks = self.parameters['horizonTicksPrivateState']
        market_horizon_ticks = self.parameters['horizonTicksMarketState']
        candle_horizon = self.parameters['horizonCandlesState']
        #     "otherInstrumentsStates": [],
        #     "otherInstrumentsMsPeriods": []
        return StateUtils._get_market_state_columns(
            private_horizon_ticks=private_horizon_ticks,
            market_horizon_ticks=market_horizon_ticks,
            candle_horizon=candle_horizon,
        )

    def _get_ta_state_columns(self):
        is_binary = False
        if "binaryStateOutputs" in self.parameters.keys():
            is_binary = self.parameters["binaryStateOutputs"] > 0

        horizonTicksMarketState = DEFAULT_MARKET_HORIZON_SAVE
        if "horizonTicksMarketState" in self.parameters.keys():
            horizonTicksMarketState = self.parameters["horizonTicksMarketState"]

        periods = DEFAULT_TA_INDICATORS_PERIODS
        if is_binary:
            periods = DEFAULT_TA_INDICATORS_PERIODS_BINARY
        if "periodsTAStates" in self.parameters.keys():
            periods = self.parameters["periodsTAStates"]

        multimarket_instruments = self.parameters['otherInstrumentsStates']
        multimarket_periods = self.parameters['otherInstrumentsMsPeriods']

        return StateUtils._get_ta_state_columns(
            binary_outputs=is_binary,
            market_horizon_save=horizonTicksMarketState,
            periods=periods,
            multimarket_instruments=multimarket_instruments,
            multimarket_periods=multimarket_periods,
        )

    # get states
    def get_dump_filename(self):
        if self.parameters['dumpFilename'] is not "null":
            return self.parameters['dumpFilename']
        return rf"dumpData_{self.algorithm_info}.csv"

    def get_dump_data_path(self):
        return LAMBDA_OUTPUT_PATH + os.sep + self.get_dump_filename()

    def get_dump_from_data(
        self, dump_path: str = None, raw_data: bool = False
    ) -> pd.DataFrame:
        if dump_path is None:
            dump_path = self.get_dump_data_path()

        state_columns = self._get_default_state_columns()
        dump_df = pd.read_csv(dump_path)
        output_columns = self.parameters["dumpSeconds"]
        assert dump_df.shape[1] == (
            len(state_columns) + 2 + output_columns * 2
        )  # output midprice and timestamp + entry timetsmp+entry idprice

        if raw_data:
            return dump_df

        columns_to_change = dump_df.columns[1 : len(state_columns) + 1]
        assert len(columns_to_change) == len(state_columns)
        dict_change = dict(zip(columns_to_change, state_columns))
        dump_df.rename(columns=dict_change, inplace=True)

        dump_df["timestamp"] = pd.to_datetime(dump_df["timestamp"], unit='ms')
        for column in list(dump_df.columns):
            if column.startswith("endTimestamp"):
                dump_df[column] = pd.to_datetime(dump_df[column], unit='ms')
        dump_df = dump_df.set_index("timestamp").sort_index(ascending=True)

        return dump_df

    def get_dump_state_columns(self) -> list:
        return self._get_default_state_columns()

    def generate_dump_data(
        self,
        start_date: datetime.datetime,  # included
        end_date: datetime.datetime,  # not included
        instrument_pk: str,
        n_jobs: int = 1,
        dumpSeconds: int = 30,
    ) -> pd.DataFrame:
        class Worker:
            def __init__(
                self,
                algorithm: TrendPredictor,
                parameters: dict,
                instrument_pk: str,
                start_date,
                end_date,
                counter=0,
            ):
                self.algorithm = algorithm.copy()
                self.algorithm.algorithm_info += rf'_dump_{counter}'

                self.parameters = copy.copy(parameters)
                self.algorithm.set_parameters(self.parameters)
                self.start_date = start_date
                self.end_date = end_date
                self.instrument_pk = instrument_pk
                self.output_df = None

            def is_finished(self):
                return self.output_df is not None

            def run_df(self):
                self.algorithm.test(
                    start_date=self.start_date,
                    end_date=self.end_date,
                    instrument_pk=self.instrument_pk,
                )
                try:
                    self.output_df = self.algorithm.get_dump_from_data(raw_data=True)
                    print(
                        rf"finished generate_dump_data {self.start_date}-{self.end_date} with {self.output_df.shape[0]} rows and {self.output_df.shape[1]} columns"
                    )
                    os.remove(
                        self.algorithm.get_dump_data_path()
                    )  # remove the dump file of this job
                except Exception as e:
                    print(rf"something failed getting dump_df ")
                    self.output_df = None

        date = start_date
        parameters_original = copy.copy(self.parameters)
        print(
            rf"generating dump data of {(end_date - start_date).days} days in n_jobs:{n_jobs}"
        )
        workers = []
        # in parallel

        while date.replace(hour=0, minute=0, second=0) < end_date.replace(
            hour=0, minute=0, second=0
        ):
            parameters = copy.copy(parameters_original)
            parameters['dumpData'] = 1
            parameters['dumpSeconds'] = dumpSeconds
            filename = rf"dumpData_{date_to_string(date)}_{self.algorithm_info}.csv"
            parameters['dumpFilename'] = filename

            next_start_date = date + datetime.timedelta(days=1)
            end_date_iteration = date.replace(
                hour=end_date.hour, minute=end_date.minute
            )
            worker = Worker(
                algorithm=self,
                parameters=parameters,
                start_date=date,
                end_date=end_date_iteration,
                instrument_pk=instrument_pk,
                counter=len(workers),
            )
            workers.append(worker)

            date = next_start_date
        jobs = []
        for work in workers:
            job = {"func": work.run_df}
            jobs.append(job)

        from factor_investing.util.paralellization_util import process_jobs_joblib

        if n_jobs < 0:
            cpu_number = os.cpu_count()
            max_threads = cpu_number * 2
            n_jobs = max_threads - n_jobs

        num_threads = min(n_jobs, len(jobs))
        process_jobs_joblib(jobs=jobs, num_threads=num_threads)

        # get outputs
        output_df = None
        for work in workers:
            day_df = work.output_df
            if day_df is None:
                print(
                    rf"{work.start_date} - {work.end_date} with no dump data! -> skip it "
                )
                continue
            if output_df is None:
                output_df = day_df
            else:
                output_df = join_by_row(output_df, day_df)
        if output_df is None or len(output_df) == 0:
            print(rf"ERROR not dump_df generated between {start_date} and {end_date}!!")
            return None

        output_df.sort_values(
            by='timestamp', ascending=True, inplace=True, ignore_index=True
        )

        self.set_parameters(parameters_original)
        dump_path = LAMBDA_OUTPUT_PATH + os.sep + self.get_dump_filename()
        print(
            rf"generate_dump_data complete with {output_df.shape[0]} rows and {output_df.shape[1]} columns dump data in {dump_path}"
        )
        output_df.to_csv(dump_path, index=False)

        return output_df

    def parameter_tuning(
        self,
        start_date: datetime.datetime,
        end_date: datetime,
        instrument_pk: str,
        parameters_min: dict,
        parameters_max: dict,
        max_simultaneous: int,
        generations: int,
        ga_configuration: Type[GAConfiguration],
        parameters_base: dict = DEFAULT_PARAMETERS,
        clean_initial_generation_experience: bool = True,
        algorithm_enum=AlgorithmEnum.rsi_dqn,
    ) -> (dict, pd.DataFrame):
        return super().parameter_tuning(
            algorithm_enum=AlgorithmEnum.trend_predictor,
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            parameters_base=parameters_base,
            parameters_min=parameters_min,
            parameters_max=parameters_max,
            max_simultaneous=max_simultaneous,
            generations=generations,
            ga_configuration=ga_configuration,
            clean_initial_generation_experience=clean_initial_generation_experience,
        )

    def get_model_path(self, verb: str, index: int = 0):
        # BUY_0_trendpredictor.onnx
        output = rf"{LAMBDA_OUTPUT_PATH}{os.sep}{self.algorithm_info}{os.sep}{verb.upper()}_{index}.onnx"
        os.makedirs(os.path.dirname(output), exist_ok=True)
        return output

    def save_normalizer_onnx_model(
        self, normalizer, input_len: int, filepath: str = None
    ):
        if filepath is None:
            filepath = rf"{LAMBDA_OUTPUT_PATH}{os.sep}{self.algorithm_info}{os.sep}normalizer.onnx"
        os.makedirs(os.path.dirname(filepath), exist_ok=True)

        import onnxmltools
        from skl2onnx.common.data_types import FloatTensorType

        initial_type = [('float_input', FloatTensorType([None, input_len]))]

        onnx_model = onnxmltools.convert_sklearn(
            normalizer, initial_types=initial_type, target_opset=7
        )
        print(rf"saving scaler into {filepath}")
        onnxmltools.utils.save_model(onnx_model, filepath)

    def save_keras_onnx_model(self, model, filepath: str, input_len: int):
        # https://github.com/onnx/onnxmltools
        import onnxmltools
        from onnxconverter_common import FloatTensorType

        initial_types = [("float_input", FloatTensorType([0, input_len]))]
        # from tf2onnx.tfonnx import process_tf_graph
        input_names = ['float_input_%s' % str(i) for i in range(model.inputs)]
        output_names = ['float_output_%s' % str(i) for i in range(model.outputs)]

        print("Inputs:", input_names)
        print("Outputs:", output_names)
        # with tf.Session() as sess:
        #     onnx_graph = process_tf_graph(sess.graph, input_names=input_names, output_names=output_names)
        print(rf"saving keras into {filepath}")
        onnx_model = onnxmltools.convert_tensorflow(
            model, input_names=input_names, output_names=output_names, target_opset=7
        )
        onnxmltools.utils.save_model(onnx_model, filepath)

    def test(
        self,
        start_date: datetime.datetime,
        end_date: datetime,
        instrument_pk: str,
        explore_prob: float = 0.0,
        trainingPredictIterationPeriod: int = None,
        trainingTargetIterationPeriod: int = None,
        algorithm_number: int = None,
        clean_experience: bool = False,
    ) -> dict:
        return super().test(
            start_date, end_date, instrument_pk, algorithm_number, clean_experience
        )

    # def get_parameters(self) -> dict:
    #     return super().get_parameters()

    def copy(self):
        return copy.deepcopy(self)


if __name__ == '__main__':
    import os

    os.environ[
        'LAMBDA_OUTPUT_PATH'
    ] = rf'E:\Usuario\Coding\Python\market_making_fw\python_lambda\output_temp'
    os.environ[
        'LAMBDA_LOGS_PATH'
    ] = rf'E:\Usuario\Coding\Python\market_making_fw\python_lambda\log_temp'

    instrument_pk = 'eurusd_darwinex'
    algorithm_info = '%s_test' % instrument_pk
    trend_predictor = TrendPredictor(algorithm_info=algorithm_info)
    # change params
    parameters = trend_predictor.get_parameters()
    parameters["dumpData"] = 1
    trend_predictor.DELAY_MS = 0

    trend_predictor.set_parameters(parameters)
    ## PT
    parameters_base_pt = DEFAULT_PARAMETERS

    print('Starting testing')
    # parameters_test['trainingPredictIterationPeriod'] = IterationsPeriodTime.END_OF_SESSION
    # parameters_test['trainingTargetIterationPeriod'] = IterationsPeriodTime.END_OF_SESSION

    iterations = 0

    trend_predictor.generate_dump_data(
        instrument_pk=instrument_pk,
        start_date=datetime.datetime(year=2022, day=11, month=5, hour=8),
        end_date=datetime.datetime(year=2022, day=13, month=5, hour=12),
        n_jobs=5,
    )

    dump_data_df = trend_predictor.get_dump_from_data()
    print(rf"{dump_data_df.shape}")
