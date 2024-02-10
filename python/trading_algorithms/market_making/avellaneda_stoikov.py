import datetime
import copy
from trading_algorithms.algorithm import Algorithm, AlgorithmParameters
from trading_algorithms.algorithm_enum import AlgorithmEnum
from backtest.backtest_launcher import BacktestLauncher, BacktestLauncherController
from backtest.input_configuration import (
    BacktestConfiguration,
    AlgorithmConfiguration,
    InputConfiguration,
    JAR_PATH,
    MultiThreadConfiguration,
)
import os
import copy
import pandas as pd

from backtest.parameter_tuning.ga_configuration import GAConfiguration


class KCalculationEnum:
    Quotes = "Quotes"
    Alridge = "Alridge"
    Pct = 'Pct'


class SpreadCalculationEnum:
    Avellaneda = "Avellaneda"
    Alridge = "Alridge"
    GueantTapia = "GueantTapia"


class AvellanedaStoikovParameters:
    skew = 'skew'
    risk_aversion = 'riskAversion'
    midprice_period_seconds = 'midpricePeriodSeconds'
    midprice_period_window = 'midpricePeriodWindow'
    seconds_change_k = 'changeKPeriodSeconds'
    k_default = "kDefault"
    a_default = 'aDefault'
    spread_calculation = 'spreadCalculation'
    k_calculation = 'kCalculation'
    position_multiplier = 'positionMultiplier'
    spread_multiplier = 'spreadMultiplier'
    calculate_Tt = 'calculateTt'
    sigma_default = 'sigmaDefault'


DEFAULT_PARAMETERS = {
    # Avellaneda default
    AvellanedaStoikovParameters.skew: (0),
    AvellanedaStoikovParameters.risk_aversion: (0.68),
    AvellanedaStoikovParameters.midprice_period_seconds: (
        60
    ),  # for midPrice variance calculation
    AvellanedaStoikovParameters.midprice_period_window: (15),
    # for midPrice variance calculation , 15 windows of 60 seconds
    AlgorithmParameters.quantity: (0.0001),
    AlgorithmParameters.first_hour: (0),
    AlgorithmParameters.last_hour: (24),
    AlgorithmParameters.ui: 0,
    AvellanedaStoikovParameters.calculate_Tt: 0,
    # if 1 reserve price effect will be less affected by position with the session time
    AvellanedaStoikovParameters.seconds_change_k: (60),
    AvellanedaStoikovParameters.k_default: (-1),
    AvellanedaStoikovParameters.a_default: (-1),
    AvellanedaStoikovParameters.sigma_default: (-1),
    AvellanedaStoikovParameters.spread_calculation: SpreadCalculationEnum.Avellaneda,
    AvellanedaStoikovParameters.k_calculation: KCalculationEnum.Pct,
    AvellanedaStoikovParameters.position_multiplier: (
        1.0
    ),  # not modify original results  1/quantity
    AvellanedaStoikovParameters.spread_multiplier: (1.0),
}


class AvellanedaStoikov(Algorithm):
    NAME = AlgorithmEnum.avellaneda_stoikov

    def __init__(self, algorithm_info: str, parameters: dict = None):

        if parameters is None:
            parameters = copy.copy(DEFAULT_PARAMETERS)

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

    avellaneda_stoikov.MULTITHREAD_CONFIGURATION = MultiThreadConfiguration.multithread
    avellaneda_stoikov.DELAY_MS = 0.0
    avellaneda_stoikov.FEES_COMMISSIONS_INCLUDED = False

    output_test = avellaneda_stoikov.test(
        instrument_pk='btcusdt_kraken',
        start_date=datetime.datetime(year=2023, day=9, month=11, hour=7),
        end_date=datetime.datetime(year=2023, day=9, month=11, hour=15),
    )

    name_output = avellaneda_stoikov.get_test_name(name=avellaneda_stoikov.NAME)
    backtest_df = output_test[name_output]
    avellaneda_stoikov.plot_trade_results(backtest_df)

    # import matplotlib.pyplot as plt
    # plt.show()
