from numba import jit
from numba.typed import List
import numpy as np
import json
from gym_zmq.zmq_env_manager import ZmqEnvManager


@jit(nopython=True)
def insert_list_and_extend(list_to_extend: List, index: int, value) -> List:
    new_list = list_to_extend[:index]
    new_list.append(value)
    new_list.extend(list_to_extend[index:])
    return new_list


class ContinuousActionAdaptor:
    def __init__(
            self,
            constant_continuous_action_index_value: dict,
            low_actions: list,
            high_actions: list,
            dtype,
            mean_centered: bool,
    ):
        self.mean_centered = mean_centered
        self.constant_continuous_action_index_value = (
            constant_continuous_action_index_value
        )
        assert len(low_actions) == len(high_actions)
        self.low_actions = np.array(low_actions)
        self.high_actions = np.array(high_actions)
        self.mean_actions = (
                                    np.array(self.low_actions) + np.array(self.high_actions)
                            ) / 2
        self.distance_actions = np.array(self.high_actions) - np.array(self.low_actions)
        self.dtype = dtype

    def get_low_high_centered_actions(self):
        if not self.mean_centered:
            return self.low_actions, self.high_actions

        low_out = []
        high_out = []
        for index in range(len(self.low_actions)):

            low_centered = self.low_actions[index] - self.mean_actions[index]
            low_centered = low_centered  # / self.distance_actions[index]
            if isinstance(self.low_actions[index], ZmqEnvManager.INT_TYPE):
                low_centered = ZmqEnvManager.INT_TYPE(np.floor(low_centered))

            high_centered = self.high_actions[index] - self.mean_actions[index]
            high_centered = high_centered  # / self.distance_actions[index]

            if isinstance(self.high_actions[index], ZmqEnvManager.INT_TYPE):
                high_centered = ZmqEnvManager.INT_TYPE(np.ceil(high_centered))

            low_out.append(low_centered)
            high_out.append(high_centered)

        return low_out, high_out

    def save(self, path):
        import pickle

        with open(path, 'wb') as handle:
            pickle.dump(self, handle, protocol=pickle.HIGHEST_PROTOCOL)

    #
    @staticmethod
    def load(path):
        import pickle

        # load the constant_continuous_action_index_value dict from a json file in path
        with open(path, 'rb') as handle:
            return pickle.load(handle)

    def adapt_action(self, action_in: np.ndarray):
        # demean the action_out
        if self.mean_centered:
            action_out = action_in * self.distance_actions + self.mean_actions

            action_out = action_out.astype(self.dtype)
            action_out = np.maximum(action_out, self.low_actions)
            action_out = np.minimum(action_out, self.high_actions)
        else:
            action_out = action_in

        action_list = List(action_out.tolist())

        for index, value in self.constant_continuous_action_index_value.items():
            action_list = insert_list_and_extend(action_list, index, value)
        action_out = np.array(action_list)

        return action_out

    def _step_wrapper(self, step, action: np.ndarray):
        if (
                self.constant_continuous_action_index_value is not None
                and len(self.constant_continuous_action_index_value) > 0
        ):
            action = self.adapt_action(action)
        return step(action)

    def step_wrapper(self, step):
        wrapper_function = lambda action: self._step_wrapper(step, action)
        return wrapper_function
