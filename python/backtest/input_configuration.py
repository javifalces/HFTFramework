import datetime
import json
import uuid
import os

from configuration import BACKTEST_JAR_PATH

'''
 * 	"backtest": {
 * 		"startDate": "20201208",
 * 		"endDate": "20201208",
 * 		"instrument": "btcusdt_binance"
 *        },
 * 	"algorithm": {
 * 		"algorithmName": "AvellanedaStoikov",
 * 		"parameters": {
 * 			"risk_aversion": "0.9",
 * 			"position_multiplier": "100",
 * 			"window_tick": "100",
 * 			"minutes_change_k": "10",
 * 			"quantity": "0.0001",
 * 			"k_default": "0.00769",
 * 			"spread_multiplier": "5.0",
 * 			"first_hour": "7",
 * 			"last_hour": "19"
 *        }
 *    }
 *
 * }'''

format_date = '%Y%m%d'
format_date_hour = '%Y%m%d %H:%M:%S'
import pandas as pd

JAR_PATH = BACKTEST_JAR_PATH


class MultiThreadConfiguration:
    multithread = "multi_thread"
    singlethread = "single_thread"


class BacktestConfiguration:
    def __init__(
            self,
            start_date: datetime.datetime,
            end_date: datetime.datetime,
            instrument_pk: str,
            delay_order_ms: int = 65,
            multithread_configuration: str = MultiThreadConfiguration.multithread,
            fees_commissions_included: bool = True,
            bucle_run: bool = False,
            seed: int = None,
    ):
        '''

        Parameters
        ----------
        start_date
        end_date
        instrument_pk
        delay_order_ms
        multithread_configuration
        fees_commissions_included
        bucle_run: if true , backtest in java is going to bucle running ,for training gym
        seed:
        '''
        self.delay_order_ms = delay_order_ms
        self.start_date = start_date
        self.end_date = end_date
        self.instrument_pk = instrument_pk
        self.multithread_configuration = multithread_configuration
        self.fees_commissions_included = fees_commissions_included
        self.bucle_run = bucle_run
        self.initial_sleep_seconds = -1
        self.seed = seed

    @staticmethod
    def read_json(json_input: str):
        json_dict = json.loads(json_input)
        try:
            start_date = datetime.datetime.strptime(json_dict['startDate'], format_date)
        except:
            start_date = datetime.datetime.strptime(
                json_dict['startDate'], format_date_hour
            )
        try:
            end_date = datetime.datetime.strptime(json_dict['endDate'], format_date)
        except:
            end_date = datetime.datetime.strptime(
                json_dict['endDate'], format_date_hour
            )
        if 'testingGym' not in json_dict.keys():
            json_dict['testingGym'] = False

        return BacktestConfiguration(
            start_date=start_date,
            end_date=end_date,
            instrument_pk=json_dict['instrument'],
            delay_order_ms=json_dict['delayOrderMs'],
            multithread_configuration=json_dict['multithreadConfiguration'],
            fees_commissions_included=json_dict['feesCommissionsIncluded'],
            bucle_run=json_dict['bucleRun'],
        )

    def get_json(self) -> str:
        output_dict = {}
        if self.start_date == self.end_date:
            # day format
            print('deprecated format date for backtest! include hours')
            output_dict['startDate'] = self.start_date.strftime(format_date)
            output_dict['endDate'] = self.end_date.strftime(format_date)
        else:
            # hour format
            output_dict['startDate'] = self.start_date.strftime(format_date_hour)
            output_dict['endDate'] = self.end_date.strftime(format_date_hour)

        output_dict['bucleRun'] = self.bucle_run
        output_dict['initialSleepSeconds'] = self.initial_sleep_seconds
        output_dict['instrument'] = self.instrument_pk
        output_dict['delayOrderMs'] = self.delay_order_ms
        output_dict['multithreadConfiguration'] = self.multithread_configuration
        output_dict['feesCommissionsIncluded'] = self.fees_commissions_included
        json_object = json.dumps(output_dict)
        return json_object


def get_parameters_string(parameters: dict):
    parameters_out = {}
    for parameter_key in parameters.keys():
        value = parameters[parameter_key]
        if parameter_key == 'algorithms':
            # for portfolio algos
            algorithm_configuration_list = value
            value_list = []
            for algorithm_configuration_dict in algorithm_configuration_list:
                algorithm_configuration = AlgorithmConfiguration(
                    algorithm_name=algorithm_configuration_dict['algorithmName'],
                    parameters=algorithm_configuration_dict['parameters'],
                )
                # value_list.append(algorithm_configuration)
                str_json = algorithm_configuration.get_json_dict()
                value_list.append(str_json)
                # value+=algorithm_configuration.get_json()+",\n"
            value = value_list

        elif isinstance(value, list):
            value = ''.join([str(elem) + ',' for elem in value])[:-1]
        else:
            value = str(value)
        parameters_out[parameter_key] = value
    return parameters_out


class AlgorithmConfiguration:
    def __init__(self, algorithm_name: str, parameters: dict):
        self.algorithm_name = algorithm_name
        self.parameters = parameters

    @staticmethod
    def read_json(json_input: str):
        json_dict = json.loads(json_input)
        return AlgorithmConfiguration(
            algorithm_name=json_dict['algorithmName'],
            parameters=json_dict['parameters'],
        )

    def get_json(self) -> str:
        output_dict = self.get_json_dict()
        json_object = json.dumps(output_dict)
        return json_object

    def get_json_dict(self) -> dict:
        output_dict = {}
        output_dict['algorithmName'] = self.algorithm_name
        output_dict[
            'parameters'
        ] = self.parameters  # get_parameters_string(self.parameters)
        return output_dict


class InputConfiguration:
    def __init__(
            self,
            backtest_configuration: BacktestConfiguration,
            algorithm_configuration: AlgorithmConfiguration,
    ):
        self.backtest_configuration = backtest_configuration
        self.algorithm_configuration = algorithm_configuration

        self._check_params()

    def _check_params(self):
        from trading_algorithms.algorithm import AlgorithmParameters

        first_hour_trading = int(
            self.algorithm_configuration.parameters[AlgorithmParameters.first_hour]
        )
        last_hour_trading = int(
            self.algorithm_configuration.parameters[AlgorithmParameters.last_hour]
        )
        we_are_not_operating = (
                self.backtest_configuration.end_date.hour < first_hour_trading
        )
        if we_are_not_operating:
            print(
                rf"WARNING: {self.algorithm_configuration.algorithm_name} is not operating at {self.backtest_configuration.end_date} hour {self.backtest_configuration.end_date.hour} < firstHour: {first_hour_trading}"
            )

    @staticmethod
    def copy_from_input_configuration(
            input_configuration: 'InputConfiguration',
            rl_host: str = None,
            rl_port: int = None,
            algorithm_name: str = None,
            seed: int = None,
    ) -> 'InputConfiguration':
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
        )
        from trading_algorithms.algorithm import AlgorithmParameters

        output = InputConfiguration(
            backtest_configuration=input_configuration.backtest_configuration,
            algorithm_configuration=input_configuration.algorithm_configuration,
        )
        if rl_host is not None:
            output.algorithm_configuration.parameters[
                RlAlgorithmParameters.rl_host
            ] = rl_host
        if rl_port is not None:
            output.algorithm_configuration.parameters[
                RlAlgorithmParameters.rl_port
            ] = rl_port
        if algorithm_name is not None:
            output.algorithm_configuration.algorithm_name = algorithm_name
        if seed is not None:
            output.algorithm_configuration.parameters[AlgorithmParameters.seed] = seed
            output.backtest_configuration.seed = seed

        return output

    @staticmethod
    def read_json(json_input: str):
        json_dict = json.loads(json_input)
        return InputConfiguration(
            backtest_configuration=BacktestConfiguration.read_json(
                json.dumps(json_dict['backtest'])
            ),
            algorithm_configuration=AlgorithmConfiguration.read_json(
                json.dumps(json_dict['algorithm'])
            ),
        )

    def get_json(self) -> str:
        json_backtest__object = rf'"backtest":{self.backtest_configuration.get_json()}'
        json_algo__object = rf'"algorithm":{self.algorithm_configuration.get_json()}'
        return '{' + json_backtest__object + ',\n' + json_algo__object + "}"

    def get_filename(self):
        return (
                self.algorithm_configuration.algorithm_name
                + '_'
                + str(uuid.uuid1())
                + '.json'
        )
