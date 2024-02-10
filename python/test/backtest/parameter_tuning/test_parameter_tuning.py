import unittest
from backtest.parameter_tuning.ga_configuration import GAConfiguration
from backtest.parameter_tuning.ga_parameter_tuning import GAParameterTuning
from trading_algorithms.algorithm import Algorithm
import numpy as np
import pandas as pd
from trading_algorithms.algorithm_enum import *
import math
import datetime
from backtest.backtest_launcher import BacktestConfiguration
import copy

DEFAULT_PARAMETERS = {
    # ConstantSpread default
    "x": (0.0001),
    "y": (0.0001),
}


def optimization_function(x: float, y: float) -> float:
    # https://www.geeksforgeeks.org/three-dimensional-plotting-in-python-using-matplotlib/
    # return np.sin(np.sqrt(x ** 2 + y ** 2))
    return -(x**2.0 + y**2.0)


class GAParameterTuningFunction(GAParameterTuning):
    def __init__(
            self,
            ga_configuration: GAConfiguration,
            algorithm: AlgorithmEnum,
            parameters_base: dict,
            parameters_min: dict,
            parameters_max: dict,
            max_simultaneous: int,
            initial_param_dict_list: list = [],
    ):
        super().__init__(
            ga_configuration,
            algorithm,
            parameters_base,
            parameters_min,
            parameters_max,
            max_simultaneous,
            initial_param_dict_list,
        )
        self.function = optimization_function

        self.population_df = pd.DataFrame(columns=['param_dict', 'score'])
        self.population_df_out = pd.DataFrame(columns=['param_dict', 'score'])

    def _run_backtests(
            self, backtest_configuration: BacktestConfiguration, param_dicts: list
    ):
        scores = []
        generations = []
        for param_dict in param_dicts:
            score = self.function(param_dict["x"], param_dict["y"])
            scores.append(score)
            generations.append(self.generation)

        new_generation_df = pd.DataFrame(columns=['param_dict', 'score'])

        new_generation_df['generation'] = generations
        new_generation_df['score'] = scores
        new_generation_df['param_dict'] = param_dicts

        self.population_df = new_generation_df
        self.population_df_out = pd.concat(
            [self.population_df_out, new_generation_df], ignore_index=True
        )

        if self.INCREASE_POPULATION:
            self.population_df = self.population_df_out

        if isinstance(self.decay, float):
            self.sigma -= self.decay

        if isinstance(self.decay, list):
            index_to_take = self.generation
            if index_to_take >= len(self.decay):
                index_to_take = len(self.decay) - 1
            self.sigma = self.ga_configuration.sigma - self.decay[index_to_take]
            self.sigma = max(self.sigma, 0.01)

        self.generation += 1


GAConfigurationMock = GAConfiguration()
GAConfigurationMock.population = 100


class MockAlgorithm(Algorithm):
    def __init__(
            self, algorithm_info: str, parameters: dict = DEFAULT_PARAMETERS
    ) -> None:
        super().__init__(algorithm_info, parameters)

    def parameter_tuning(
            self,
            algorithm_enum: AlgorithmEnum = 'MockAlgorithm',
            start_date: datetime.datetime = None,
            end_date: datetime = None,
            instrument_pk: str = None,
            parameters_base: dict = {"x": -5.00, "y": 2.5},
            parameters_min: dict = {"x": -10.00, "y": -10.00},
            parameters_max: dict = {"x": 10.00, "y": 10.00},
            max_simultaneous: int = 0,
            generations: int = 10,
            ga_configuration: GAConfiguration = GAConfigurationMock,
            clean_initial_generation_experience: bool = True,
    ) -> (dict, pd.DataFrame):
        # exponential reducing sigma
        generations_exp_decay = []
        exp_multiplier = ga_configuration.exp_multiplier
        sigmas = [ga_configuration.sigma]
        for generation in range(1, generations):
            sigma_expected = ga_configuration.sigma * math.exp(
                -exp_multiplier * generation
            )
            generations_exp_decay.append(ga_configuration.sigma - sigma_expected)
            sigmas.append(ga_configuration.sigma - generations_exp_decay[-1])
        sigmas_str = [str(i) for i in sigmas]
        print(rf"Sigmas parameter_tuning -> {sigmas_str}")
        ga_configuration.decay = generations_exp_decay

        ga_parameter_tuning = GAParameterTuningFunction(
            ga_configuration=ga_configuration,
            algorithm=algorithm_enum,
            parameters_base=copy.copy(parameters_base),
            parameters_min=copy.copy(parameters_min),
            parameters_max=copy.copy(parameters_max),
            max_simultaneous=max_simultaneous,
            initial_param_dict_list=[],
        )

        best_param_dict = {}
        for generation in range(generations):
            ga_parameter_tuning.run_generation(
                backtest_configuration=None,
                check_unique_population=True,
            )
            best_param_dict = ga_parameter_tuning.get_best_param_dict()

            best_score = ga_parameter_tuning.get_best_score()
        print(
            '%s best score %.4f\nbest_param_dict %s'
            % (algorithm_enum, best_score, best_param_dict)
        )
        return (best_param_dict, ga_parameter_tuning.population_df_out, sigmas)


class TestParameterTuning(unittest.TestCase):
    def test_optimization(self):
        algorithm = MockAlgorithm("mockIt")
        best_param_dict, population_df_out, sigmas = algorithm.parameter_tuning()
        self.assertTrue(sigmas[0] > sigmas[-1])
        # best is 0.0 0.0 due to optimization function
        self.assertAlmostEqual(best_param_dict['x'], 0.0, delta=0.6)
        self.assertAlmostEqual(best_param_dict['y'], 0.0, delta=0.6)
