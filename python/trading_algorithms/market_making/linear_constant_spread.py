import datetime
from typing import Type

from trading_algorithms.algorithm import Algorithm, AlgorithmParameters
from trading_algorithms.algorithm_enum import AlgorithmEnum
from backtest.backtest_launcher import BacktestLauncher, BacktestLauncherController
from backtest.input_configuration import (
    BacktestConfiguration,
    AlgorithmConfiguration,
    InputConfiguration,
    JAR_PATH,
)
import os
import copy
import pandas as pd

from backtest.parameter_tuning.ga_configuration import GAConfiguration


class LinearConstantSpreadParameters:
    quantity_limit = 'quantityLimit'
    level = 'level'  # 0 - 4


DEFAULT_PARAMETERS = {
    AlgorithmParameters.quantity: (0.0001),
    AlgorithmParameters.first_hour: (0),
    AlgorithmParameters.last_hour: (24),
    AlgorithmParameters.ui: 0,
    LinearConstantSpreadParameters.level: (0),
    LinearConstantSpreadParameters.quantity_limit: (0.001),  # x10
}


class LinearConstantSpread(Algorithm):
    NAME = AlgorithmEnum.linear_constant_spread

    def __init__(self, algorithm_info: str, parameters: dict = DEFAULT_PARAMETERS):
        super().__init__(
            algorithm_info=self.NAME + "_" + algorithm_info, parameters=parameters
        )

    def get_parameters(self) -> dict:
        parameters = copy.copy(self.parameters)
        return parameters

    def train(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            iterations: int,
            algos_per_iteration: int,
            simultaneous_algos: int = 1,
    ) -> list:
        # makes no sense

        backtest_configuration = BacktestConfiguration(
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            delay_order_ms=self.DELAY_MS,
            multithread_configuration=self.MULTITHREAD_CONFIGURATION,
            fees_commissions_included=self.FEES_COMMISSIONS_INCLUDED,
        )
        output_list = []
        for iteration in range(iterations):
            backtest_launchers = []
            for algorithm_number in range(algos_per_iteration):
                parameters = self.parameters
                algorithm_name = '%s_%s_%d' % (
                    self.NAME,
                    self.algorithm_info,
                    algorithm_number,
                )
                algorithm_configurationQ = AlgorithmConfiguration(
                    algorithm_name=algorithm_name, parameters=parameters
                )
                input_configuration = InputConfiguration(
                    backtest_configuration=backtest_configuration,
                    algorithm_configuration=algorithm_configurationQ,
                )

                backtest_launcher = BacktestLauncher(
                    input_configuration=input_configuration,
                    id=algorithm_name,
                    jar_path=JAR_PATH,
                )
                backtest_launchers.append(backtest_launcher)

            if iteration == 0 and os.path.isdir(backtest_launchers[0].output_path):
                # clean it
                self.clean_experience(output_path=backtest_launchers[0].output_path)

            # Launch it
            backtest_controller = BacktestLauncherController(
                backtest_launchers=backtest_launchers,
                max_simultaneous=simultaneous_algos,
            )
            output_dict = backtest_controller.run()
            output_list.append(output_dict)

        return output_list

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
    ) -> (dict, pd.DataFrame):

        return super().parameter_tuning(
            algorithm_enum=self.NAME,
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            parameters_base=parameters_base,
            parameters_min=parameters_min,
            parameters_max=parameters_max,
            max_simultaneous=max_simultaneous,
            generations=generations,
            ga_configuration=ga_configuration,
        )


if __name__ == '__main__':
    constant_spread = LinearConstantSpread(algorithm_info='test_main')

    # ga_configuration = GAConfiguration
    # ga_configuration.population = 3
    # best_param_dict, summary_df = constant_spread.parameter_tuning(
    #     instrument_pk='btcusdt_binance',
    #     start_date=datetime.datetime(year=2020, day=9, month=12),
    #     end_date=datetime.datetime(year=2020, day=9, month=12),
    #     parameters_min={"quantity_limit": 5, "level": 0},
    #     parameters_max={"quantity_limit": 25, "level": 4},
    #     generations=3,
    #     max_simultaneous=1,
    #     ga_configuration=ga_configuration,
    # )
    # constant_spread.set_parameters(parameters=best_param_dict)
    #
    # output_test = constant_spread.test(
    #     instrument_pk='btcusdt_binance',
    #     start_date=datetime.datetime(year=2020, day=9, month=12),
    #     end_date=datetime.datetime(year=2020, day=9, month=12),
    # )

    output_test = constant_spread.test(
        instrument_pk='btcusdt_binance',
        start_date=datetime.datetime(year=2020, day=19, month=12, hour=7),
        end_date=datetime.datetime(year=2020, day=19, month=12, hour=19),
    )
