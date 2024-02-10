import numpy
import numpy as np


def list_value(value, size):
    input_list = np.ones(size) * value
    return input_list.tolist()


def list_to_str(
        input_list: list,
        sep: str = ',',
) -> str:
    # Convert floats to strings
    string_list = [str(num) for num in input_list]
    # Join the strings with commas
    result = sep.join(string_list)
    return result


def create_random_list(size: int) -> list:
    input_list = np.random.random(size).tolist()
    return input_list


def create_zeros_list(size: int) -> list:
    input_list = np.zeros(size).tolist()
    return input_list


def list_to_numpy(input_list: list) -> numpy.array:
    numeric_list = [float(num) for num in input_list]
    return np.array(numeric_list)
