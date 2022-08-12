import datetime

from trading_algorithms.algorithm import Algorithm
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


class KCalculationEnum:
    Quotes = "Quotes"
    Alridge = "Alridge"


class SpreadCalculationEnum:
    Avellaneda = "Avellaneda"
    Alridge = "Alridge"
    GueantTapia = "GueantTapia"


DEFAULT_PARAMETERS = {
    # Avellaneda default
    "riskAversion": (0.68),
    "windowTick": (25),  # for midPrice variance calculation
    "quantity": (0.0001),
    "firstHour": (7),
    "lastHour": (19),
    "calculateTt": 1,  # if 1 reserve price effect will be less affected by position with the session time
    "minutesChangeK": (1),
    "kDefault": (-1),
    "aDefault": (-1),
    "sigmaDefault": (-1),
    "spreadCalculation": SpreadCalculationEnum.Avellaneda,
    "kCalculation": KCalculationEnum.Alridge,
    "positionMultiplier": (335.35),  # not modify original results  1/quantity
    "spreadMultiplier": (5.0),
}


class AvellanedaStoikov(Algorithm):
    NAME = AlgorithmEnum.avellaneda_stoikov

    def __init__(self, algorithm_info: str, parameters: dict = DEFAULT_PARAMETERS):
        parameters = Algorithm.set_defaults_parameters(
            parameters=parameters, DEFAULT_PARAMETERS=DEFAULT_PARAMETERS
        )
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

    def test(
        self,
        start_date: datetime.datetime,
        end_date: datetime,
        instrument_pk: str,
        algorithm_number: int = 0,
        clean_experience: bool = False,
    ) -> dict:
        backtest_configuration = BacktestConfiguration(
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            delay_order_ms=self.DELAY_MS,
            multithread_configuration=self.MULTITHREAD_CONFIGURATION,
            fees_commissions_included=self.FEES_COMMISSIONS_INCLUDED,
        )
        parameters = self.get_parameters()
        algorithm_name = self.get_test_name(
            name=self.NAME, algorithm_number=algorithm_number
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

        if clean_experience:
            self.clean_experience(output_path=backtest_launcher.output_path)

        backtest_controller = BacktestLauncherController(
            backtest_launchers=[backtest_launcher], max_simultaneous=1
        )
        output_dict = backtest_controller.run()
        output_dict[self.algorithm_info] = output_dict[algorithm_name]
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
        ga_configuration: GAConfiguration,
        parameters_base: dict = None,
    ) -> (dict, pd.DataFrame):

        if parameters_base is None:
            parameters_base = self.parameters

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
    avellaneda_stoikov.parameter_tuning(
        start_date=datetime.datetime(year=2022, day=14, month=6),
        end_date=datetime.datetime(year=2022, day=14, month=6),
        parameters_min={"kDefault": 0.0},
        parameters_max={"kDefault": 1.0},
        instrument_pk='btcusdt_binance',
        max_simultaneous=1,
        generations=5,
        ga_configuration=GAConfiguration(),
    )
    new_parameters = copy.copy(DEFAULT_PARAMETERS)
    new_parameters["spreadCalculation"] = SpreadCalculationEnum.GueantTapia
    new_parameters["kDefault"] = 0.3
    new_parameters["aDefault"] = 0.9
    new_parameters["calculateTt"] = 0.0
    avellaneda_stoikov.set_parameters(new_parameters)

    output_test = avellaneda_stoikov.test(
        instrument_pk='btcusdt_binance',
        start_date=datetime.datetime(year=2022, day=14, month=6),
        end_date=datetime.datetime(year=2022, day=14, month=6),
    )
    name_output = avellaneda_stoikov.get_test_name(name=avellaneda_stoikov.NAME)
    backtest_df = output_test[name_output]
    avellaneda_stoikov.plot_trade_results(backtest_df)
    # import matplotlib.pyplot as plt
    # plt.show()
