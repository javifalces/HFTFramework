import datetime
from typing import Type
import copy

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
import pandas as pd
from backtest.parameter_tuning.ga_configuration import GAConfiguration
from backtest.score_enum import ScoreEnum


DEFAULT_PARAMETERS = {
    # Q
    "skewPricePctAction": [0.0],
    "riskAversionAction": [0.9, 0.5],
    "windowsTickAction": [5, 10],
    "minPrivateState": (-0.001),
    "maxPrivateState": (0.001),
    "numberDecimalsPrivateState": (4),
    "horizonTicksPrivateState": (1),
    "horizonMinMsTick": (10),
    "scoreEnum": ScoreEnum.total_pnl,
    "timeHorizonSeconds": (5),
    "epsilon": (0.2),
    # Avellaneda default
    "risk_aversion": (0.9),
    "position_multiplier": (100),
    "window_tick": (10),
    "minutes_change_k": (10),
    "quantity": (0.0001),
    "k_default": (0.00769),
    "spread_multiplier": (5.0),
    "first_hour": (7),
    "last_hour": (19),
}


class AvellanedaQ(Algorithm):
    NAME = AlgorithmEnum.avellaneda_q

    def __init__(self, algorithm_info: str, parameters: dict = DEFAULT_PARAMETERS):
        super().__init__(
            algorithm_info=self.NAME + "_" + algorithm_info, parameters=parameters
        )

    def get_parameters(self, explore_prob: float)->dict:
        parameters = copy.copy(self.parameters)
        parameters['explore_prob'] = explore_prob
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

        backtest_configuration = BacktestConfiguration(
            start_date=start_date, end_date=end_date, instrument_pk=instrument_pk
        )
        output_list = []

        for iteration in range(iterations):
            backtest_launchers = []
            for algorithm_number in range(algos_per_iteration):
                parameters = self.get_parameters(
                    explore_prob=1 - iteration / iterations
                )

                self.set_training_seed(parameters=parameters, iteration=iteration, algorithm_number=algorithm_number)

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
            # in case number of states/actions changes
            self.clean_permutation_cache(output_path=backtest_launchers[0].output_path)

            # Launch it
            backtest_controller = BacktestLauncherController(
                backtest_launchers=backtest_launchers,
                max_simultaneous=simultaneous_algos,
            )
            output_dict = backtest_controller.run()
            output_list.append(output_dict)
            # Combine experience
            if algos_per_iteration>1:
                self.merge_q_matrix(backtest_launchers=backtest_launchers)
        return output_list

    def test(
        self,
        start_date: datetime.datetime,
        end_date: datetime,
        instrument_pk: str,
        explore_prob: float = 0.2,
        algorithm_numer: int = 0,
        clean_experience: bool = False,
    ) -> dict:
        backtest_configuration = BacktestConfiguration(
            start_date=start_date, end_date=end_date, instrument_pk=instrument_pk
        )
        parameters = self.get_parameters(explore_prob=explore_prob)
        algorithm_name = '%s_%s_%d' % (self.NAME, self.algorithm_info, algorithm_numer)

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
            algorithm_enum=AlgorithmEnum.avellaneda_q,
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

    avellaneda_q = AvellanedaQ(algorithm_info='test_main')

    ga_configuration = GAConfiguration
    ga_configuration.population = 3

    output_train = avellaneda_q.parameter_tuning(
        instrument_pk='btcusdt_binance',
        start_date=datetime.datetime(year=2020, day=8, month=12),
        end_date=datetime.datetime(year=2020, day=8, month=12),
        parameters_min={"risk_aversion": 0.1, "window_tick": 3},
        parameters_max={"risk_aversion": 0.9, "window_tick": 15},
        generations=3,
        max_simultaneous=1,
        ga_configuration=ga_configuration,


    )


    output_train = avellaneda_q.train(
        instrument_pk='btcusdt_binance',
        start_date=datetime.datetime(year=2020, day=8, month=12),
        end_date=datetime.datetime(year=2020, day=8, month=12),
        iterations=5,
        algos_per_iteration=2,
        simultaneous_algos=1,
    )

    output_test = avellaneda_q.test(
        instrument_pk='btcusdt_binance',
        start_date=datetime.datetime(year=2020, day=9, month=12),
        end_date=datetime.datetime(year=2020, day=9, month=12),
    )
