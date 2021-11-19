import pandas as pd
import numpy as np
import random
import copy

MIN_CROSSOVER_PARENT_PCT = 0.2
MAX_CROSSOVER_PARENT_PCT = 0.8


def random_param_dict(
    sigma: float,
    scale_ser: pd.Series,
    min_ser: pd.Series,
    max_ser: pd.Series,
    current_eval_ser=None,
) -> dict:
    # randomizer between min and max
    if current_eval_ser is None:
        eval_ser = copy.copy(max_ser) * np.random.random(max_ser.size)
        sigma = 2
    else:
        eval_ser = copy.copy(current_eval_ser)
        sigma = sigma

    try:
        new_np = np.random.normal(scale_ser * 0.0, sigma, len(scale_ser)) * scale_ser
        # except Exception as e:
        #     raise e
        eval_ser += new_np

        eval_ser.dropna(inplace=True)
        if len(eval_ser)!=len(max_ser) or len(eval_ser)!=len(min_ser):
            print(f'eval_ser is different len than max_ser or min_ser  -> {eval_ser}')
        eval_ser[eval_ser > max_ser] = max_ser[eval_ser > max_ser]
        eval_ser[eval_ser < min_ser] = min_ser[eval_ser < min_ser]
    except Exception as e:
        raise e
    return eval_ser.to_dict()


def crossover_param_dict(parameter_dict_a: dict, parameter_dict_b: dict):
    percent_a = random.random()
    percent_a = max(MIN_CROSSOVER_PARENT_PCT, percent_a)
    percent_a = min(MAX_CROSSOVER_PARENT_PCT, percent_a)
    series_a = pd.Series(parameter_dict_a) * percent_a

    percent_b = 1 - percent_a
    series_b = pd.Series(parameter_dict_b) * percent_b
    offspring = series_a + series_b
    return offspring.to_dict()


def get_weights(population_df: pd.DataFrame, by: str = 'score'):
    weights = population_df[by]
    weights += abs(min(weights))  # avoid neg weights
    return weights
