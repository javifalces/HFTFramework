import datetime
import random
from pathlib import Path

import pandas as pd
from optuna import Study

from backtest.input_configuration import (
    BacktestConfiguration,
    AlgorithmConfiguration,
    InputConfiguration,
)
import torch as th
from configuration import LAMBDA_OUTPUT_PATH
from database.tick_db import TickDB
from gym_zmq.envs.market_making_backtest_env import MarketMakingBacktestEnv
from trading_algorithms.algorithm import AlgorithmParameters
from trading_algorithms.algorithm_enum import AlgorithmEnum
from trading_algorithms.dqn_algorithm import DQNAlgorithm

from trading_algorithms.iterations_period_time import IterationsPeriodTime
from trading_algorithms.reinforcement_learning.continuous_action_adaptor import (
    ContinuousActionAdaptor,
)

from trading_algorithms.score_enum import ScoreEnum
from trading_algorithms.state_utils import StateType
from utils.list_utils import list_value
import numpy as np
import sys
import os

np.seterr(invalid='ignore')

import os
import matplotlib.pyplot as plt
import numpy as np


class ReinforcementLearningActionType:
    discrete = "discrete"
    continuous = "continuous"


class BaseModelType:
    PPO = 'PPO'
    DQN = 'DQN'
    SAC = 'SAC'
    A2C = 'A2C'
    TD3 = 'TD3'
    DDPG = 'DDPG'
    HER = 'HER'


class ModelPolicy:
    MlpPolicy = 'MlpPolicy'
    CnnPolicy = 'CnnPolicy'


class InfoStepKey:
    # com.lambda.investing.algorithmic_trading.reinforcement_learning.SingleInstrumentRLAlgorithm#getInfo
    totalPnl = "totalPnl"
    realizedPnl = "realizedPnl"
    unrealizedPnl = "unrealizedPnl"
    cumDayReward = "cumDayReward"
    diffReward = "diffReward"
    reward = "reward"


class RlAlgorithmParameters:
    launch_tensorboard = "launch_tensorboard"
    normalize_clip_obs = "normalize_clip_obs"
    max_batch_size = "maxBatchSize"
    batch_size = "batchSize"
    training_predict_iteration_period = "trainingPredictIterationPeriod"
    training_target_iteration_period = "trainingTargetIterationPeriod"
    epoch = "epoch"
    learning_rate_nn = "learningRateNN"
    learning_rate_decrease = "learningRateDecrease"

    score = "scoreEnum"
    exploration_probability = "epsilon"
    discount_factor = "gamma"

    state_horizon_min_ms = "horizonMinMsTick"
    state_filter = "stateColumnsFilter"
    state_number_decimals = "numberDecimalsState"

    min_private_state = 'minPrivateState'
    max_private_state = 'maxPrivateState'
    number_decimals_private_state = 'numberDecimalsPrivateState'
    horizon_ticks_private_state = 'horizonTicksPrivateState'
    min_market_state = 'minMarketState'
    max_market_state = 'maxMarketState'
    number_decimals_market_state = 'numberDecimalsMarketState'
    horizon_ticks_market_state = 'horizonTicksMarketState'
    min_candle_state = 'minCandleState'
    max_candle_state = 'maxCandleState'
    number_decimals_candle_state = 'numberDecimalsCandleState'
    horizon_candles_state = 'horizonCandlesState'
    horizon_min_ms_tick = 'horizonMinMsTick'
    score_enum = 'scoreEnum'
    step_seconds = 'stepSeconds'

    state_candles_length = "horizonCandlesState"

    state_binary = "binaryStateOutputs"
    state_ta_indicators_periods = "periodsTAStates"

    state_other_instruments = "otherInstrumentsStates"
    state_other_instruments_period_ms = "otherInstrumentsMsPeriods"
    state_min_market_tick_ms = "marketTickMs"
    volume_candles = "volumeCandles"
    second_candles = "secondsCandles"
    candle_type_business = "candleTypeBusiness"
    step_max_time_waiting_action_results_ms = "maxTimeWaitingActionResultsMs"
    step_max_time_waiting_exit_ms = "maxTimeWaitingExitMs"
    seed = "seed"

    training_stats = "trainingStats"
    action_type = "reinforcementLearningActionType"
    model = "baseModel"
    stop_action_on_filled = "stopActionOnFilled"
    model_policy = "policy"
    rl_host = "rlHost"
    rl_port = "rlPort"
    vf_coef = "vf_coef"  # 0.5
    ent_coef = "ent_coef"  # 0.0 https://www.youtube.com/watch?v=1ppslywmIPs&t=510s
    gae_lambda = "gae_lambda"  # 0.95
    clip_range = "clip_range"  # 0.2

    core_model_kwargs = "core_model_kwargs"
    device = 'device'

    nn_hidden_nodes_multiplier = "hidden_nodes_multiplier"
    nn_hidden_layers = "nn_hidden_layers"
    nn_activation_fn = "nn_activation_fn"


DEFAULT_RL_PORT = 3000
DEFAULT_RL_HOST = 'localhost'

DEFAULT_PARAMETERS = {
    AlgorithmParameters.quantity: (0.0001),
    AlgorithmParameters.first_hour: (0),
    AlgorithmParameters.last_hour: (24),
    AlgorithmParameters.ui: 0,
    #
    RlAlgorithmParameters.normalize_clip_obs: 40,  # 10.0 if >0 we are going to normalize observations

    RlAlgorithmParameters.training_stats: False,  # to enable training UI interface on localhost:9000
    # RL parameters
    RlAlgorithmParameters.max_batch_size: 1_000_000,  # buffer_size DQN/SAC 1000000 300000
    RlAlgorithmParameters.batch_size: 64,  # batch_size 32 - 256 - 512
    RlAlgorithmParameters.training_predict_iteration_period: IterationsPeriodTime.TWO_HOURS,
    # train only at the end,offline
    RlAlgorithmParameters.training_target_iteration_period: IterationsPeriodTime.FOUR_HOURS,
    # train at the end,offline
    RlAlgorithmParameters.epoch: 10,  # for PPO
    RlAlgorithmParameters.learning_rate_nn: 3e-4,  # learning_rate:between 0.01 to 0.1
    RlAlgorithmParameters.learning_rate_decrease: False,
    RlAlgorithmParameters.state_horizon_min_ms: (0),
    RlAlgorithmParameters.score: ScoreEnum.realized_pnl,
    RlAlgorithmParameters.exploration_probability: (0.05),
    # exploration_final_eps  probability of explore=> random action
    RlAlgorithmParameters.discount_factor: 0.000001,
    # 0.99,  # gamma : next state prediction reward discount.is the value to the future rewards
    # RlAlgorithmParameters.learning_rate: 0.95,  # 0.25 in phd? new values reward multiplier
    RlAlgorithmParameters.state_binary: 0,
    RlAlgorithmParameters.state_filter: [],
    RlAlgorithmParameters.seed: 0,
    RlAlgorithmParameters.second_candles: 56,
    # we dont need to change it! its inferred by base model
    RlAlgorithmParameters.action_type: ReinforcementLearningActionType.discrete,
    RlAlgorithmParameters.state_other_instruments: [],
    RlAlgorithmParameters.state_other_instruments_period_ms: [],
    RlAlgorithmParameters.stop_action_on_filled: 0,
    RlAlgorithmParameters.model: BaseModelType.PPO,  # DQN ,PPO , SAC
    RlAlgorithmParameters.model_policy: ModelPolicy.MlpPolicy,  # MlpPolicy , CnnPolicy
    RlAlgorithmParameters.rl_host: DEFAULT_RL_HOST,
    RlAlgorithmParameters.rl_port: DEFAULT_RL_PORT,

    RlAlgorithmParameters.device: 'auto',
    RlAlgorithmParameters.vf_coef: 0.5,
    RlAlgorithmParameters.ent_coef: 0.0,  # https://www.youtube.com/watch?v=1ppslywmIPs&t=510s
    RlAlgorithmParameters.gae_lambda: 0.95,
    RlAlgorithmParameters.clip_range: 0.2,
    RlAlgorithmParameters.core_model_kwargs: None,
    RlAlgorithmParameters.launch_tensorboard: False,
    RlAlgorithmParameters.nn_hidden_layers: -1,
    RlAlgorithmParameters.nn_hidden_nodes_multiplier: -1,
    RlAlgorithmParameters.nn_activation_fn: None,
}


class RlPaths:
    def __init__(self, complete_name: str):
        self.complete_name = complete_name
        self.agent_model_path = RLAlgorithm.get_agent_model_path(complete_name)
        self.agent_model_checkpoint_path = RLAlgorithm.get_agent_model_checkpoint_path(
            complete_name
        )
        self.base_output_path = LAMBDA_OUTPUT_PATH + rf'/{complete_name}'
        self.agent_onnx_path = (
                LAMBDA_OUTPUT_PATH + rf'/{complete_name}/agent_model.onnx'
        )
        self.normalizer_model_path = RLAlgorithm.get_normalizer_model_path(
            complete_name
        )
        self.normalizer_json_path = (
                LAMBDA_OUTPUT_PATH + rf'/{complete_name}/normalizer_model.json'
        )
        self.continuous_action_adaptor_path = RLAlgorithm.get_action_adaptor_path(
            complete_name
        )
        self.tensorboard_log = LAMBDA_OUTPUT_PATH + rf'/{complete_name}/tensorboard_log'

    def __reduce__(self):
        return (self.__class__, (self.complete_name))


class RLAlgorithm(DQNAlgorithm):
    MAX_INT = 9223372036854775807
    VERBOSE_MODEL = 1
    SAME_VALUE_CONTINUOUS_ACTION_DELTA = 1e-6
    TRAIN_BUCLE_RUN = False  # If True , training is not going to finish java process .TODO : fix sometimes desync with java process

    def __init__(self, algorithm_info: str, parameters: dict) -> None:
        super().__init__(algorithm_info, parameters)

        self.continuous_action_adaptor: ContinuousActionAdaptor = None

        self.counter_maker = 0
        self.env_id = MarketMakingBacktestEnv.ENVIRONMENT_ID
        # get dict value from parameters if exists else use default value DEFAULT_TEST_MODE

        self._configure_core_rl_algorithm(
            algorithm_info=algorithm_info, parameters=parameters
        )
        self._set_env_parameters()
        self.algorithm_configuration = AlgorithmConfiguration(
            algorithm_name=self.algorithm_name, parameters=self.parameters
        )
        self.model = None

    def __reduce__(self):
        return (self.__class__, (self.algorithm_info, self.parameters))

    def _configure_core_rl_algorithm(self, algorithm_info: str, parameters: dict):
        self.algorithm_info = algorithm_info
        self._set_rl_paths()

        from trading_algorithms.reinforcement_learning.core.core_rl_algorithm import (
            CoreRlAlgorithm,
        )
        from configuration import get_reinforcement_learning_framework
        self._set_reinforcement_learning_action_type()

        state_columns = self.get_number_of_state_columns(parameters)
        action_columns = self.get_number_of_action_columns(parameters)
        self.core_rl_algorithm = CoreRlAlgorithm.create_core(
            get_reinforcement_learning_framework(),
            algorithm_info,
            parameters,
            self.rl_paths
        )
        self.base_model_str = self.core_rl_algorithm.base_model_str

    def get_reinforcement_learning_action_type(self):
        reinforcement_learning_action_type = ReinforcementLearningActionType.discrete
        if self.core_rl_algorithm._continuous_action_model():
            reinforcement_learning_action_type = (
                ReinforcementLearningActionType.continuous
            )
        return reinforcement_learning_action_type

    def _set_reinforcement_learning_action_type(self):
        if self.parameters[RlAlgorithmParameters.action_type] is None:
            reinforcement_learning_action_type = (
                self.get_reinforcement_learning_action_type()
            )
        else:
            reinforcement_learning_action_type = self.parameters[
                RlAlgorithmParameters.action_type
            ]

        self.parameters[
            RlAlgorithmParameters.action_type
        ] = reinforcement_learning_action_type
        self.reinforcement_learning_action_type = reinforcement_learning_action_type

    def _save_model(self, model, env, config=None):
        # to use in python
        print(
            rf"Saving rl_algorithm {self.reinforcement_learning_action_type} model to {self.rl_paths.agent_model_path}"
        )
        self.core_rl_algorithm.save_model(env, model, config)

        if self.continuous_action_adaptor is not None:
            self.continuous_action_adaptor.save(self.continuous_action_adaptor_path)

    def clean_model(self):
        self.core_rl_algorithm.clean_model()

    def test(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            explore_prob: float = 0.2,
            trainingPredictIterationPeriod: int = None,
            trainingTargetIterationPeriod: int = None,
            algorithm_number: int = None,
            clean_experience: bool = False,
    ) -> dict:

        self.change_state_type_if_required(instrument_pk, True)

        return self._test_backtest(
            start_date,
            end_date,
            instrument_pk,
            explore_prob,
            trainingPredictIterationPeriod,
            trainingTargetIterationPeriod,
            algorithm_number,
            clean_experience,
        )

    def set_parameters(self, parameters: dict):
        super().set_parameters(parameters)

        self._configure_core_rl_algorithm(
            algorithm_info=self.algorithm_info, parameters=self.parameters
        )

        self._set_env_parameters(print_it=True)

    def change_state_type_if_required(self, instrument_pk: str, print_it: bool = False):
        if (
                TickDB().is_fx_instrument(instrument_pk)
                and self.state_type == StateType.ta_state
        ):
            print(
                rf"changing state type to {StateType.ta_state_fx} for {self.algorithm_name}"
            )
            self.state_type = StateType.ta_state_fx
            self._set_env_parameters(print_it=print_it)

        if (
                not TickDB().is_fx_instrument(instrument_pk)
                and self.state_type == StateType.ta_state_fx
        ):
            print(
                rf"changing state type to {StateType.ta_state} for {self.algorithm_name}"
            )
            self.state_type = StateType.ta_state
            self.state_columns = self.get_number_of_state_columns(self.parameters, print_it=print_it)  # 38

    @staticmethod
    def next_multiple(number, factor):
        if number % factor == 0:
            return number
        return number + (factor - number % factor)

    def parameter_tuning_training(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            iterations: int,
            parameters_min: dict,
            parameters_max: dict,
            optuna_configuration: 'OptunaConfiguration',
            parameters_base,
            simultaneous_algos: int = 1,
            clean_initial_experience: bool = True,
            tb_log: bool = False,
            clean_previous_tb: bool = False,
            validation_start_date: datetime.datetime = None,
            validation_end_date: datetime = None,
    ) -> Study:

        def objective(trial, validation_start_date=None, validation_end_date=None):
            parameters = parameters_base.copy()
            parameters[RlAlgorithmParameters.launch_tensorboard] = False  # disable to avoid problems

            parameters_str_list = []
            for key in parameters_min:
                min_value = parameters_min[key]
                max_value = parameters_max[key]
                base_value = parameters_base[key]
                is_integer = isinstance(base_value, int) and isinstance(min_value, int) and isinstance(max_value, int)
                is_bool = isinstance(base_value, bool) and isinstance(min_value, bool) and isinstance(max_value, bool)
                is_str = (isinstance(base_value, str) and
                          (isinstance(min_value, str) or isinstance(min_value, list)) and
                          (isinstance(max_value, str) or isinstance(max_value, list)))

                if is_integer:
                    parameters[key] = trial.suggest_int(key, parameters_min[key], parameters_max[key])
                elif is_bool:
                    parameters[key] = trial.suggest_categorical(key, [True, False])
                elif is_str:
                    min_value = parameters_min[key]
                    max_value = parameters_max[key]
                    if isinstance(min_value, str):
                        min_value = [min_value]
                    if isinstance(max_value, str):
                        max_value = [max_value]
                    categoricals = list(set(min_value + max_value))
                    parameters[key] = trial.suggest_categorical(key, categoricals)
                else:
                    parameters[key] = trial.suggest_float(key, parameters_min[key], parameters_max[key])

                parameters_str_list.append(rf"{key}_{parameters[key]}")

            # change rl port
            original_port = parameters[RlAlgorithmParameters.rl_port]

            parameters[RlAlgorithmParameters.rl_port] = original_port + trial.number * 2
            # print(rf"starting trial.number:{trial.number} with port: {parameters[RlAlgorithmParameters.rl_port]}")
            algorithm_name = self.algorithm_name + rf"_{trial.number}"
            algorithm_configuration = AlgorithmConfiguration(
                algorithm_name=algorithm_name, parameters=parameters
            )


            self.change_state_type_if_required(instrument_pk, True)
            # create backtest_configuration
            backtest_configuration = BacktestConfiguration(
                start_date=start_date,
                end_date=end_date,
                instrument_pk=instrument_pk,
                delay_order_ms=self.DELAY_MS,
                multithread_configuration=self.MULTITHREAD_CONFIGURATION,
                fees_commissions_included=self.FEES_COMMISSIONS_INCLUDED,
                bucle_run=self.TRAIN_BUCLE_RUN,
            )

            input_configuration = InputConfiguration(
                backtest_configuration=backtest_configuration,
                algorithm_configuration=algorithm_configuration,
            )
            env_config = self._create_env_config(
                input_configuration=input_configuration, is_training=True
            )

            from trading_algorithms.reinforcement_learning.core.core_rl_algorithm import (
                CoreRlAlgorithm,
            )
            from configuration import get_reinforcement_learning_framework
            tb_log_name = None
            if tb_log:
                tb_log_name = '_'.join(parameters_str_list)
                parameters[RlAlgorithmParameters.training_stats] = 1

            else:
                parameters[RlAlgorithmParameters.training_stats] = 0

            core_rl_algorithm = CoreRlAlgorithm.create_core(
                get_reinforcement_learning_framework(),
                self.algorithm_info + rf"_{trial.number}",
                parameters,
                self.rl_paths,
            )

            model, env, all_infos = core_rl_algorithm.train(
                env_config=env_config,
                iterations=iterations,
                simultaneous_algos=simultaneous_algos,
                min_iterations=iterations,
                plot_training=False,
                tb_log_name=tb_log_name,
                clean_initial_experience=True,
                use_validation_callback=False
            )
            # if plot_training:
            #     training_fig_path = (
            #             LAMBDA_OUTPUT_PATH
            #             + rf'/{self.algorithm_info}/training_figures/{tb_log_name}.png'
            #     )
            #     Path(training_fig_path).parent.mkdir(parents=True, exist_ok=True)
            #     plt.gcf()
            #     plt.savefig(training_fig_path)
            #     plt.close()
            print(rf"parameter_tuning save_model : {core_rl_algorithm.rl_paths.agent_model_path}")
            core_rl_algorithm.save_model(env, model, config=None)

            if validation_start_date is None:
                validation_start_date = start_date
            if validation_end_date is None:
                validation_end_date = end_date

            backtest_configuration_validation = BacktestConfiguration(
                start_date=validation_start_date,
                end_date=validation_end_date,
                instrument_pk=instrument_pk,
                delay_order_ms=self.DELAY_MS,
                multithread_configuration=self.MULTITHREAD_CONFIGURATION,
                fees_commissions_included=self.FEES_COMMISSIONS_INCLUDED,
                bucle_run=self.TRAIN_BUCLE_RUN,
            )

            input_configuration_validation = InputConfiguration(
                backtest_configuration=backtest_configuration_validation,
                algorithm_configuration=algorithm_configuration,
            )

            env_config = self._create_env_config(
                input_configuration=input_configuration_validation, is_training=False
            )
            print(rf"parameter_tuning test from model : {core_rl_algorithm.rl_paths.agent_model_path}")
            model, env, all_infos_validation = core_rl_algorithm.test(env_config=env_config)

            last_info_dict = all_infos_validation[-1]  # take last iteration info
            try:
                score = float(last_info_dict[optuna_configuration.score_column])
            except:
                print(rf"validation exception data -> return 0")
                score = 0


            return score

        if clean_previous_tb:
            tensorboard_path = self.rl_paths.tensorboard_log
            if os.path.exists(tensorboard_path):
                print(rf"clean_previous_tb at {tensorboard_path}")
                import shutil
                shutil.rmtree(tensorboard_path)

        import optuna
        seed = int(
            self.parameters.get(RlAlgorithmParameters.seed, random.randint(0, 10000))
        )
        if optuna_configuration.sampler_kwargs is not None:
            sampler = optuna_configuration.sampler(seed=seed, **optuna_configuration.sampler_kwargs)
            # create it to read it in objective
            if 'search_space' in optuna_configuration.sampler_kwargs:
                parameters_min = {}
                parameters_max = {}
                for key in optuna_configuration.sampler_kwargs['search_space']:
                    parameters_min[key] = min(optuna_configuration.sampler_kwargs['search_space'][key])
                    parameters_max[key] = max(optuna_configuration.sampler_kwargs['search_space'][key])
        else:
            sampler = optuna_configuration.sampler(seed=seed)
            from optuna.samplers import GridSampler
            if optuna_configuration.sampler is GridSampler:
                search_space = {}
                for key in parameters_min:
                    min_value = parameters_min[key]
                    max_value = parameters_max[key]
                    mean_value = (min_value + max_value) / 2
                    search_space[key] = [min_value, mean_value, max_value]
                sampler = optuna_configuration.sampler(seed=seed, search_space=search_space)
            else:
                sampler = optuna_configuration.sampler(seed=seed)

        load_if_exists = not clean_initial_experience

        if simultaneous_algos > 1:
            print("disable output console")

            old_stdout = sys.stdout  # backup current stdout
            sys.stdout = open(os.devnull, "w")

        study = optuna.create_study(direction=optuna_configuration.direction,
                                    sampler=sampler,
                                    study_name=self.algorithm_name,
                                    load_if_exists=load_if_exists
                                    )

        study.optimize(lambda trial: objective(trial, validation_start_date, validation_end_date),
                       n_trials=optuna_configuration.n_trials, n_jobs=optuna_configuration.n_jobs)
        if simultaneous_algos > 1:
            sys.stdout = old_stdout  # reset old stdout

        print("Best value: {} (params: {})\n".format(study.best_value, study.best_params))

        if tb_log:
            from tensorboard import program
            tb = program.TensorBoard()
            tb.configure(argv=[None, '--logdir', self.rl_paths.tensorboard_log])
            url = tb.launch()
            print(f"Tensorflow / Tensorboard listening on url {url}")

        return study

    def train(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            iterations: int,
            simultaneous_algos: int = 1,
            score_early_stopping: str = InfoStepKey.totalPnl,
            patience: int = -1,
            clean_initial_experience: bool = False,
            min_iterations: int = None,
            start_date_validation: datetime.datetime = None,
            end_date_validation: datetime = None,
            plot_training: bool = True,
            tb_log_name: str = None,
            use_validation_callback: bool = True,
    ) -> list:

        '''

        Parameters
        ----------
        start_date : datetime.datetime of training the agent
        end_date : datetime.datetime of training the agent
        instrument_pk : instrument to train the agent
        iterations: max number of episodes, in parallel will reach faster
        simultaneous_algos: parallel training only with PPO,SAC baseModel its going to speed up learning
        score_early_stopping: scoreEnum to check improvement at the end of each episode
        patience: patience to early stop training
        clean_initial_experience: delete the model and normalizer model in the begining
        min_iterations: number of min episodes to run , in parallel will reach faster
        plot_training: plot training results after each episode
        tb_log_name: custom name for tensorboard log
        end_date_validation : for validation callback we are going to save best agent on this data, if None is going to be same date as training
        start_date_validation : for validation callback we are going to save best agent on this data, if None is going to be same date as training
        Returns list with all infos end of each episode and last element is df with all infos
        -------

        '''
        import os

        number_cpus = os.cpu_count()
        if simultaneous_algos < 0:
            simultaneous_algos = number_cpus + simultaneous_algos
        if simultaneous_algos > number_cpus:
            simultaneous_algos = number_cpus

        if iterations % simultaneous_algos != 0:
            iterations = self.next_multiple(iterations, simultaneous_algos)
            print(
                rf"adapt iterations to {iterations} to be multiple of {simultaneous_algos}"
            )
        if min_iterations is not None and min_iterations % simultaneous_algos != 0:
            min_iterations = self.next_multiple(min_iterations, simultaneous_algos)
            print(
                rf"adapt min_iterations to {min_iterations} to be multiple of {simultaneous_algos}"
            )

        self.change_state_type_if_required(instrument_pk, True)
        # create backtest_configuration
        backtest_configuration = BacktestConfiguration(
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            delay_order_ms=self.DELAY_MS,
            multithread_configuration=self.MULTITHREAD_CONFIGURATION,
            fees_commissions_included=self.FEES_COMMISSIONS_INCLUDED,
            bucle_run=self.TRAIN_BUCLE_RUN,
        )

        input_configuration = InputConfiguration(
            backtest_configuration=backtest_configuration,
            algorithm_configuration=self.algorithm_configuration,
        )
        env_config = self._create_env_config(
            input_configuration=input_configuration, is_training=True
        )

        if start_date_validation is None:
            start_date_validation = start_date
        if end_date_validation is None:
            end_date_validation = end_date

        self.model, env, all_infos = self.core_rl_algorithm.train(
            env_config=env_config,
            iterations=iterations,
            simultaneous_algos=simultaneous_algos,
            score_early_stopping=score_early_stopping,
            patience=patience,
            clean_initial_experience=clean_initial_experience,
            min_iterations=min_iterations,
            plot_training=plot_training,
            tb_log_name=tb_log_name,
            start_date_validation=start_date_validation,
            end_date_validation=end_date_validation,
            use_validation_callback=use_validation_callback
        )
        from trading_algorithms.reinforcement_learning.core.core_rl_algorithm import SaveModelConfig

        config_save = {
            SaveModelConfig.save_env: True,
            SaveModelConfig.save_model: False,
            SaveModelConfig.save_replay_buffer: True,
        }
        if not os.path.exists(self.rl_paths.agent_model_path):
            print(rf"WARNING: model not saved at {self.rl_paths.agent_model_path} -> force save")
            config_save[SaveModelConfig.save_model] = True

        if not use_validation_callback:
            print(rf"use_validation_callback: model saved at {self.rl_paths.agent_model_path}")
            config_save[SaveModelConfig.save_model] = True

        self._save_model(self.model, env, config=config_save)

        df = self._get_df_train_results(all_infos, plot_training=plot_training)
        all_infos.append(df)  # TODO check it append
        return all_infos

    # def stop_backtest_function(self, func, *args, **kwargs):
    #     self.pause_backtest_callback.pause()
    #     output = func(*args, **kwargs)
    #     self.pause_backtest_callback.resume()
    #     return output

    def _get_df_train_results(self, all_infos: list, plot_training) -> pd.DataFrame:
        reward = []
        realized = []
        unrealized = []
        total = []
        iterations = []

        for info_dict in all_infos:
            realized_pnl = float(info_dict.get('realizedPnl', 0.0))
            unrealized_pnl = float(info_dict.get('unrealizedPnl', 0.0))
            total_pnl = float(info_dict.get('totalPnl', 0.0))
            # reward_val = float(info_dict.get('reward', 0.0))
            reward_val = float(info_dict.get('cumDayReward', 0.0))
            iterations_number = float(info_dict.get('iterations', 0.0))

            realized.append(realized_pnl)
            unrealized.append(unrealized_pnl)
            total.append(total_pnl)
            reward.append(reward_val)
            iterations.append(iterations_number)

        # create dataframe with all infos in each column
        df = pd.DataFrame(
            list(zip(reward, realized, unrealized, total, iterations)),
            columns=[
                'reward',
                'realizedPnl',
                'unrealizedPnl',
                'totalPnl',
                'iterations',
            ],
        )

        if plot_training:
            plt.close()
            try:
                color_close = 'black'
                color_open = 'darkgray'
                color_mean = 'gray'
                alpha = 0.9
                lw = 0.5

                figsize = (20, 12)
                fig, axs = plt.subplots(nrows=3, ncols=1, figsize=figsize)

                for index in range(len(axs)):
                    ax = axs[index]
                    if index == 0:
                        ax.plot(df['reward'], color=color_mean, lw=lw, alpha=alpha)
                        ax.set_ylabel('reward')
                        ax.set_xlabel('episode')
                        ax.set_title('cum reward end of episode')
                        ax.grid(axis='y', ls='--', alpha=0.7)
                    if index == 1:
                        ax.plot(
                            df['realizedPnl'], color=color_close, lw=lw, alpha=alpha
                        )
                        ax.plot(df['totalPnl'], color=color_open, lw=lw, alpha=alpha)
                        # ax.plot(df['unrealizedPnl'], color=color_open, lw=lw, alpha=alpha)
                        legend_values = ['close_pnl', 'total_pnl']
                        ax.legend(legend_values)
                        ax.set_ylabel('pnl (€)')
                        ax.set_xlabel('episode')
                        ax.set_title('pnl end of episode')
                        ax.grid(axis='y', ls='--', alpha=0.7)

                    if index == 2:
                        ax.plot(df['iterations'], color=color_close, lw=lw, alpha=alpha)
                        ax.set_ylabel('iterations')
                        ax.set_xlabel('episode')
                        ax.set_title('iterations per episode')
                        ax.grid(axis='y', ls='--', alpha=0.7)
                plt.tight_layout()
                plt.show()
            except Exception as e:
                print(rf"ERROR plotting training {self.algorithm_name} {e}")

        return df

    @staticmethod
    def get_agent_model_path(algorithm_info: str) -> str:
        agent_model_path = LAMBDA_OUTPUT_PATH + rf'/{algorithm_info}/agent_model.zip'
        return agent_model_path

    @staticmethod
    def get_agent_model_checkpoint_path(algorithm_info: str) -> str:
        agent_model_path = (
                LAMBDA_OUTPUT_PATH + rf'/{algorithm_info}/agent_model_checkpoint'
        )
        return agent_model_path

    @staticmethod
    def get_normalizer_model_path(algorithm_info: str) -> str:
        normalizer_model_path = (
                LAMBDA_OUTPUT_PATH + rf'/{algorithm_info}/normalizer_model.pkl'
        )
        return normalizer_model_path

    @staticmethod
    def get_action_adaptor_path(algorithm_info: str) -> str:
        adaptor_path = (
                LAMBDA_OUTPUT_PATH + rf'/{algorithm_info}/continuous_action_adaptor.pkl'
        )
        return adaptor_path

    def _set_rl_paths(self):
        complete_name = self.get_test_name(self.NAME)
        self.rl_paths = RlPaths(complete_name)

    def _set_env_parameters(self, print_it: bool = False):
        import gymnasium

        self.agent_model_path = self.rl_paths.agent_model_path
        self.agent_onnx_path = self.rl_paths.agent_onnx_path
        self.normalizer_model_path = self.rl_paths.normalizer_model_path
        self.normalizer_json_path = self.rl_paths.normalizer_json_path
        self.continuous_action_adaptor_path = (
            self.rl_paths.continuous_action_adaptor_path
        )
        self.tensorboard_log = self.rl_paths.tensorboard_log

        self.seed = int(
            self.parameters.get(RlAlgorithmParameters.seed, random.randint(0, 10000))
        )
        self.algorithm_name = self.get_test_name(name=self.NAME)

        self.state_columns = self.get_number_of_state_columns(self.parameters, print_it=print_it)  # 38
        self.number_of_actions = self.get_number_of_action_columns(
            self.parameters
        )  # 20

        # define states

        low_states = list_value(
            value=float('-inf'), size=self.state_columns
        )  # [-1.0, -1.0, -1.0, -1.0]
        high_states = list_value(
            value=float('inf'), size=self.state_columns
        )  # [1.0, 1.0, 1.0, 1.0]
        from gym_zmq.zmq_env_manager import ZmqEnvManager

        low_array = np.array(low_states).astype(ZmqEnvManager.FLOAT_TYPE)
        high_array = np.array(high_states).astype(ZmqEnvManager.FLOAT_TYPE)
        self.observation_space = gymnasium.spaces.Box(
            low_array, high_array, dtype=ZmqEnvManager.FLOAT_TYPE, seed=self.seed
        )

        # define actions
        if (
                self.reinforcement_learning_action_type
                == ReinforcementLearningActionType.continuous
        ):
            # continuous
            actions_list = self._get_action_columns()
            low_actions = []
            high_actions = []
            constant_continuous_action_index_value = {}
            dtype_general = ZmqEnvManager.INT_TYPE
            for index, action in enumerate(actions_list):
                low_val = action[0]
                high_val = action[1]
                if abs(low_val - high_val) < self.SAME_VALUE_CONTINUOUS_ACTION_DELTA:
                    print(
                        rf"continuous action {action} with index {index} with low_val==high_val {low_val} {high_val} -> added to ContinuousActionAdaptor"
                    )
                    # we are going to add it later when we send to the algorithm
                    constant_continuous_action_index_value[index] = low_val
                    continue
                # if low_val is float transform it to FLOAT_TYPE

                if isinstance(low_val, float):
                    dtype_general = ZmqEnvManager.FLOAT_TYPE
                    low_val = ZmqEnvManager.FLOAT_TYPE(low_val)
                if isinstance(high_val, float):
                    dtype_general = ZmqEnvManager.FLOAT_TYPE
                    high_val = ZmqEnvManager.FLOAT_TYPE(high_val)

                if isinstance(low_val, int):
                    low_val = ZmqEnvManager.INT_TYPE(low_val)
                if isinstance(high_val, int):
                    high_val = ZmqEnvManager.INT_TYPE(high_val)

                low_actions.append(low_val)
                high_actions.append(high_val)

            self.continuous_action_adaptor = ContinuousActionAdaptor(
                constant_continuous_action_index_value,
                low_actions,
                high_actions,
                dtype_general,
                mean_centered=True,
            )
            (
                low_actions,
                high_actions,
            ) = self.continuous_action_adaptor.get_low_high_centered_actions()

            number_actions = len(low_actions)
            self.action_space = gymnasium.spaces.Box(
                low=np.array(low_actions, dtype=dtype_general),
                high=np.array(high_actions, dtype=dtype_general),
                dtype=dtype_general,
                seed=self.seed,
            )
        else:
            # discrete
            self.action_space = gymnasium.spaces.Discrete(self.number_of_actions)
            number_actions = self.number_of_actions

        if print_it:
            print(
                rf"actions(PYTHON) [{number_actions}] : {self.reinforcement_learning_action_type} "
            )

    def _create_env_config(
            self, input_configuration: InputConfiguration, is_training: bool = True
    ):
        env_config = {
            'observation_space': self.observation_space,
            'action_space': self.action_space,
            'bucle_run': input_configuration.backtest_configuration.bucle_run,
            'input_configuration': input_configuration.get_json(),
            'is_training': is_training,
            'continuous_action_adaptor': self.continuous_action_adaptor,
        }

        env_config = MarketMakingBacktestEnv.modify_env_config_rl(env_config)
        return env_config

    @staticmethod
    def get_train_freq_timedelta(
            training_predict_iteration_period,
    ) -> datetime.timedelta:
        if training_predict_iteration_period == IterationsPeriodTime.OFF:
            return datetime.timedelta.max
        if training_predict_iteration_period == IterationsPeriodTime.HALF_HOUR:
            return datetime.timedelta(minutes=30)
        if training_predict_iteration_period == IterationsPeriodTime.HOUR:
            return datetime.timedelta(hours=1)
        if training_predict_iteration_period == IterationsPeriodTime.TWO_HOURS:
            return datetime.timedelta(hours=2)
        if training_predict_iteration_period == IterationsPeriodTime.THREE_HOURS:
            return datetime.timedelta(hours=3)
        if training_predict_iteration_period == IterationsPeriodTime.FOUR_HOURS:
            return datetime.timedelta(hours=4)
        if training_predict_iteration_period == IterationsPeriodTime.FIVE_HOURS:
            return datetime.timedelta(hours=5)
        if training_predict_iteration_period == IterationsPeriodTime.SIX_HOURS:
            return datetime.timedelta(hours=6)
        if training_predict_iteration_period == IterationsPeriodTime.SEVEN_HOURS:
            return datetime.timedelta(hours=7)
        if training_predict_iteration_period == IterationsPeriodTime.EIGHT_HOURS:
            return datetime.timedelta(hours=8)
        if training_predict_iteration_period == IterationsPeriodTime.DAILY:
            return datetime.timedelta(hours=24)

        if training_predict_iteration_period == IterationsPeriodTime.END_OF_SESSION:
            return datetime.timedelta.max

    def get_number_timesteps_per_episode(self) -> int:
        seconds_per_step = 1
        if (
                RlAlgorithmParameters.step_max_time_waiting_exit_ms in self.parameters
                and RlAlgorithmParameters.step_max_time_waiting_action_results_ms
                in self.parameters
        ):
            miliseconds_per_step = (
                    self.parameters[RlAlgorithmParameters.step_max_time_waiting_exit_ms]
                    * 0.25
                    + self.parameters[
                        RlAlgorithmParameters.step_max_time_waiting_action_results_ms
                    ]
                    * 0.75
            )
            seconds_per_step = np.ceil(miliseconds_per_step / 1000)
        elif RlAlgorithmParameters.step_seconds in self.parameters:
            seconds_per_step = self.parameters[RlAlgorithmParameters.step_seconds]
        else:
            print(
                rf"WARNING: step_seconds not found in parameters , using default value seconds_per_step = {seconds_per_step}"
            )
        number_hours = (
                self.parameters[AlgorithmParameters.last_hour]
                - self.parameters[AlgorithmParameters.first_hour]
        )
        if number_hours <= 0:
            number_hours += 24
        episode_length_seconds = datetime.timedelta(hours=number_hours).total_seconds()
        return episode_length_seconds / seconds_per_step

    def _get_last_backtest_output(self, env):

        try:
            backtest_env = self.core_rl_algorithm.get_backtest_env(env)
            output = backtest_env.get_last_controller_output()
        except Exception as e:
            print(rf"WARNING: error getting last backtest output {e}")
            return {}
        # TODO remove this in the future , this is a hack to make the output compatible with the old version
        name_dict = self.get_test_name(name=self.NAME)

        key_name = list(output.keys())[0]
        output[name_dict] = output[key_name]

        # this is what we really need.... the rest is only used in old dataframes
        output[self.algorithm_info] = output[key_name]

        return output

    def _test_backtest(
            self,
            start_date: datetime.datetime,
            end_date: datetime,
            instrument_pk: str,
            explore_prob: float = 0.2,
            trainingPredictIterationPeriod: int = None,
            trainingTargetIterationPeriod: int = None,
            algorithm_number: int = None,
            clean_experience: bool = False,
    ) -> dict:

        # create backtest_configuration
        backtest_configuration = BacktestConfiguration(
            start_date=start_date,
            end_date=end_date,
            instrument_pk=instrument_pk,
            delay_order_ms=self.DELAY_MS,
            multithread_configuration=self.MULTITHREAD_CONFIGURATION,
            fees_commissions_included=self.FEES_COMMISSIONS_INCLUDED,
            bucle_run=False,  # we want to finish after one iteration
        )

        input_configuration = InputConfiguration(
            backtest_configuration=backtest_configuration,
            algorithm_configuration=self.algorithm_configuration,
        )
        env_config = self._create_env_config(
            input_configuration=input_configuration, is_training=False
        )

        model, env, infos = self.core_rl_algorithm.test(env_config=env_config)
        # self._save_model(model, env)#there is nothing to save
        output = self._get_last_backtest_output(env)
        return output
