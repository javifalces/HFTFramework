import datetime
from typing import Type

from backtest.algorithm import Algorithm
from backtest.algorithm_enum import AlgorithmEnum
from backtest.backtest_launcher import BacktestLauncher, BacktestLauncherController
from backtest.input_configuration import (
    BacktestConfiguration,
    AlgorithmConfiguration,
    InputConfiguration,
    JAR_PATH,
)
import glob
import os
import copy
import pandas as pd

from backtest.parameter_tuning.ga_configuration import GAConfiguration
from backtest.score_enum import ScoreEnum

DEFAULT_PARAMETERS = {
    # Avellaneda default
    "risk_aversion": (0.68),#low means risk-neutral , high ~0.5 means risk averse investor
    "window_tick": (25),#for midPrice variance calculation
    "quantity": (0.0001),

    "first_hour": (7),
    "last_hour": (19),

    "calculateTt":1,#if 1 reserve price effect will be less affected by position with the session time
    "minutes_change_k": (1),
    "k_default": (-1),  # will calculate from market trades on last minutes_change_k
    "position_multiplier": (335.35),  # not modify original results  1/quantity
    "spread_multiplier": (5.0),
}


class AvellanedaStoikov(Algorithm):
    NAME = AlgorithmEnum.avellaneda_stoikov

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
            start_date=start_date, end_date=end_date, instrument_pk=instrument_pk
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

    def test(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            algorithm_number: int = 0,
            clean_experience: bool = False,
    ) -> dict:
        backtest_configuration = BacktestConfiguration(
            start_date=start_date, end_date=end_date, instrument_pk=instrument_pk
        )
        parameters = self.get_parameters()
        algorithm_name = self.get_test_name(name=self.NAME, algorithm_number=algorithm_number)

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

        if clean_experience:
            self.clean_experience(output_path=backtest_launcher.output_path)

        backtest_controller = BacktestLauncherController(
            backtest_launchers=[backtest_launcher], max_simultaneous=1
        )
        output_dict = backtest_controller.run()
        output_dict[self.algorithm_info]=output_dict[algorithm_name]
        del output_dict[algorithm_name]
        return output_dict

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
            algorithm_enum=AlgorithmEnum.avellaneda_stoikov,
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
    avellaneda_stoikov = AvellanedaStoikov(algorithm_info='test_main')

    ga_configuration = GAConfiguration
    ga_configuration.population = 3
    ga_configuration.decay=[0.5,0.7]
    ga_configuration.sigma=2

    best_param_dict, summary_df = avellaneda_stoikov.parameter_tuning(
        instrument_pk='btcusdt_binance',
        start_date=datetime.datetime(year=2020, day=9, month=12),
        end_date=datetime.datetime(year=2020, day=9, month=12),
        parameters_min={"risk_aversion": 0.1, "window_tick": 3},
        parameters_max={"risk_aversion": 0.9, "window_tick": 15},
        generations=3,
        max_simultaneous=1,
        ga_configuration=ga_configuration,
    )
    # avellaneda_stoikov.set_parameters(parameters=best_param_dict)
    #
    # output_test = avellaneda_stoikov.test(
    #     instrument_pk='btcusdt_binance',
    #     start_date=datetime.datetime(year=2020, day=9, month=12),
    #     end_date=datetime.datetime(year=2020, day=9, month=12),
    # )
