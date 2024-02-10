from typing import Type, Callable, Tuple, Any

import gymnasium
import torch as th
from stable_baselines3.common.base_class import BaseAlgorithm
from stable_baselines3.common.callbacks import EvalCallback

from stable_baselines3.common.policies import BaseModel
from stable_baselines3.common.vec_env import (
    VecNormalize,
    DummyVecEnv,
    SubprocVecEnv,
    VecEnv,
)
from trading_algorithms.iterations_period_time import IterationsPeriodTime
from gym_zmq import MarketMakingBacktestEnv
import datetime

from trading_algorithms.reinforcement_learning.core.baselines3.core_baselines3_callbacks import MeanRewardPrintCallback, \
    EvalSaveCustom
from trading_algorithms.reinforcement_learning.core.core_rl_algorithm import (
    CoreRlAlgorithm, SaveModelConfig,
)

import numpy as np
import os

from trading_algorithms.reinforcement_learning.rl_algorithm import (
    InfoStepKey,
    RlAlgorithmParameters, ModelPolicy, BaseModelType,
)


class CoreBaselines3(CoreRlAlgorithm):
    USE_JAX = True
    DETERMINISTIC = True

    ActivationFunction = {
        'relu': th.nn.ReLU,
        'tanh': th.nn.Tanh,
        'sigmoid': th.nn.Sigmoid
    }

    @staticmethod
    def linear_schedule(initial_value: float) -> Callable[[float], float]:
        """
        Linear learning rate schedule.

        :param initial_value: Initial learning rate.
        :return: schedule that computes
          current learning rate depending on remaining progress
        """
        if (CoreBaselines3.USE_JAX):
            print("WARNING: linear_schedule not implemented in JAX")

        def func(progress_remaining: float) -> float:
            """
            Progress will decrease from 1 (beginning) to 0.

            :param progress_remaining:
            :return: current learning rate
            """
            return progress_remaining * initial_value

        return func

    def __init__(
            self, algorithm_info: str, parameters: dict, rl_paths: 'RlPaths'
    ) -> None:
        super().__init__(algorithm_info, parameters, rl_paths)
        self._base_model = self._get_model()
        # th.autograd.set_detect_anomaly(True)
        # np.seterr(all="raise")  # define before your code.


    def train(
            self,
            env_config: dict,
            iterations: int,
            simultaneous_algos: int = 1,
            score_early_stopping: str = InfoStepKey.totalPnl,
            patience: int = -1,
            clean_initial_experience: bool = False,
            min_iterations: int = None,
            plot_training: bool = True,
            tb_log_name: str = None,
            start_date_validation: datetime.datetime = None,
            end_date_validation: datetime.datetime = None,
            use_validation_callback: bool = True,
    ) -> Tuple[BaseAlgorithm, gymnasium.Env, list]:

        if clean_initial_experience:
            self.clean_model()

        env_config['iterations'] = iterations
        env_config['algorithm_name'] = self.algorithm_info
        env0 = self._create_env(env_config, True, simultaneous_algos)

        model, env = self._create_model(
            clean_initial_experience=clean_initial_experience, env0=env0, training=True
        )

        model, all_infos = self._learn(
            model,
            env,
            env_config=env_config,
            max_episodes=iterations,
            patience=patience,
            score_early_stopping=score_early_stopping,
            min_iterations=min_iterations,
            simultaneous_algos=simultaneous_algos,
            tb_log_name=tb_log_name,
            start_date_validation=start_date_validation,
            end_date_validation=end_date_validation,
            validation_callback=use_validation_callback,
        )

        env.close()
        return model, env, all_infos

    def test(self, env_config: dict) -> Tuple[Any, gymnasium.Env, list]:

        env_config['algorithm_name'] = self.algorithm_info
        env0 = self._create_env(env_config, False, 1)
        model, env = self._create_model(
            clean_initial_experience=False, env0=env0, training=False
        )

        # run the test
        state = env.reset()  # send the start signal
        infos = []
        while True:
            try:
                # state = np.nan_to_num(state, 0.0)
                action, next_state = model.predict(state, deterministic=self.DETERMINISTIC)
                is_valid_action = action is not None and action.size > 0
            except Exception as e:
                print(
                    rf"WARNING: model.predict exception ,set random action : {e}\n state: {state} "
                )
                is_valid_action = False

            if not is_valid_action:
                state = env.observation_space.sample()
                print(rf"WARNING: action is not valid from model ")
                continue

            state, rewards, done, info = env.step(action)
            infos.append(info[0])

            # action in a numpy array list
            if done:
                print("_test_python done")
                break
        env.close()
        return model, env, infos

    def _create_model(
            self, clean_initial_experience: bool, env0, training: bool
    ):
        from trading_algorithms.reinforcement_learning.rl_algorithm import RLAlgorithm
        import os

        agent_model_path = self.rl_paths.agent_model_path
        # already loaded in _create_env
        # normalizer_model_path = self.rl_paths.normalizer_model_path

        model_exists = os.path.exists(agent_model_path)
        if not clean_initial_experience and model_exists:
            # env = self._load_env(env0, normalizer_model_path, training)
            # loading the data
            print(rf"Loading existing model from {agent_model_path} ...")
            model = self._load_model(env0)
            if self.check_model(model, env0):
                if model.env is None:
                    model.env = env0
                return model, env0
            else:
                print("WARNING: model and env are not compatible -> create new model")

        # create models
        if not training:
            print(rf"WARNING: create new model for {self.algorithm_info} in testing")
        # state is normalized vs mid price
        # env = VecNormalize(env0, norm_obs=True, norm_reward=False, training=training, clip_obs=1000., clip_reward=50.0,
        #                    gamma=0.95)  # with or without VecNormalize
        env = env0

        model = self._create_model_agent(
            env,
            seed=self.seed,
            explore_prob=self.parameters.get('epsilon', 0.05),
            training=training,
            tensorboard_log=self.tensorboard_log,
        )

        return model, env

    def _get_model(self) -> Type[BaseModel]:
        '''

        Parameters
        ----------
        base_model_str :  DQN ,PPO , SAC

        Returns
        -------

        '''
        if CoreBaselines3.USE_JAX:
            from sbx import DQN, PPO, SAC  # sbx-rl
        else:
            from stable_baselines3 import DQN, PPO, SAC

        if self.base_model_str == "DQN":
            return DQN
        elif self.base_model_str == "PPO":
            return PPO
        elif self.base_model_str == "SAC":
            return SAC
        else:
            raise ValueError(
                f"Unknown base model rl_algorithm.get_model {self.base_model_str}"
            )

    def _parallelized_training_model(self):
        from trading_algorithms.reinforcement_learning.rl_algorithm import BaseModelType
        return self.base_model_str in [BaseModelType.PPO, BaseModelType.SAC]

    def get_backtest_env(self, env) -> MarketMakingBacktestEnv:
        # TODO something better
        try:
            return env.envs[0].env.env
        except:
            return env.venv.envs[0].env.env

    def _get_steps_from_iteration_period(
            self, iteration_period: IterationsPeriodTime
    ) -> Tuple[int, str]:

        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
            RLAlgorithm,
        )

        if not IterationsPeriodTime.is_a_period(iteration_period):
            return (iteration_period, "step")

        if iteration_period == IterationsPeriodTime.END_OF_SESSION:
            return (1, "episode")
        if iteration_period == IterationsPeriodTime.OFF:
            return (self.MAX_INT, "episode")  # max int
        else:
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
                    rf"WARNING: step_seconds not found in parameters , using default value (1, episode)"
                )
                return (1, "episode")

            timespan = RLAlgorithm.get_train_freq_timedelta(iteration_period)
            seconds_timespan = timespan.total_seconds()
            steps_per_timespan = round(seconds_timespan / seconds_per_step)

            return (steps_per_timespan, "step")

    def _add_core_model_kwargs(self, model_args):
        core_model_kwargs = self.parameters.get(RlAlgorithmParameters.core_model_kwargs, None)
        if core_model_kwargs is not None and len(core_model_kwargs) > 0:
            for key, value in core_model_kwargs.items():
                model_args[key.strip()] = value

    def _get_custom_policy_kwargs(self, env, model):
        nn_hidden_layers = self.parameters.get(RlAlgorithmParameters.nn_hidden_layers, -1)
        nn_hidden_nodes_multiplier = self.parameters.get(RlAlgorithmParameters.nn_hidden_nodes_multiplier, -1)
        nn_activation_fn = self.parameters.get(RlAlgorithmParameters.nn_activation_fn, None)
        if nn_activation_fn is not None:
            nn_activation_fn = nn_activation_fn.lower()

        activation_function = self.ActivationFunction[nn_activation_fn]

        input_nodes = env.observation_space.shape[0]
        # output_nodes = env.action_space.shape[0]
        hidden_nodes = int(round(input_nodes * nn_hidden_nodes_multiplier, 0))
        layers = []
        for i in range(int(nn_hidden_layers)):
            layers.append(hidden_nodes)

        # https://stable-baselines3.readthedocs.io/en/master/guide/custom_policy.html
        print(
            f' nn_hidden_layers: {nn_hidden_layers} nn_hidden_nodes_multiplier: {nn_hidden_nodes_multiplier} nn_activation_fn: {nn_activation_fn} => len(layers): {len(layers)} of hidden_nodes: {hidden_nodes}')
        # [<shared layers>, dict(vf=[<non-shared value network layers>], pi=[<non-shared policy network layers>])]
        # net_arch = [layers[0],dict(activation_fn=activation_function)]#pi=layers, vf=layers)

        net_arch = dict(activation_fn=activation_function,
                        net_arch=layers)

        # net_arch = dict(activation_fn=activation_function,
        #                 net_arch=dict(pi=layers, vf=layers))
        # if self.base_model_str == BaseModelType.DQN:
        #     net_arch = dict(activation_fn=activation_function,
        #                     net_arch=layers)
        # if self.base_model_str == BaseModelType.SAC:
        #     net_arch = dict(activation_fn=activation_function,
        #                     net_arch=dict(pi=layers, qf=layers))

        return net_arch

    def _create_model_agent(
            self,
            env,
            seed,
            explore_prob: float,
            training: bool,
            tensorboard_log: str = None,
    ):
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
            RLAlgorithm,
        )

        # Applying the Trading RL Algorithm
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
        )

        learning_rate = self.parameters.get(
            RlAlgorithmParameters.learning_rate_nn, 1e-2
        )
        learning_rate_decrease = self.parameters.get(
            RlAlgorithmParameters.learning_rate_decrease, False
        )
        buffer_size = self.parameters.get(
            RlAlgorithmParameters.max_batch_size, 1000000
        )  # size of the replay buffer
        discount_factor = self.parameters.get(
            RlAlgorithmParameters.discount_factor, 0.99
        )
        epoch = self.parameters.get(RlAlgorithmParameters.epoch, 10)

        if explore_prob is None:
            explore_prob = self.parameters.get(
                RlAlgorithmParameters.exploration_probability, 0.05
            )

        # Update the model every train_freq steps. Alternatively pass a tuple of frequency and unit like (5, "step") or (2, "episode").
        batch_size = self.parameters.get(RlAlgorithmParameters.batch_size, 64)
        device = self.parameters.get(
            RlAlgorithmParameters.device, 'auto'
        )  # cpu or cuda if detected
        training_stats_disable = (
                self.parameters.get(RlAlgorithmParameters.training_stats, 0) == 0
        )
        policy = self.parameters.get(RlAlgorithmParameters.model_policy, ModelPolicy.MlpPolicy)

        vf_coef = self.parameters.get(RlAlgorithmParameters.vf_coef, 0.5)
        ent_coef = self.parameters.get(RlAlgorithmParameters.ent_coef, 0.0)
        clip_range = self.parameters.get(RlAlgorithmParameters.clip_range, 0.2)
        gae_lambda = self.parameters.get(RlAlgorithmParameters.gae_lambda, 0.95)

        if training_stats_disable:
            tensorboard_log = None
        else:
            from pathlib import Path

            Path(tensorboard_log).mkdir(parents=True, exist_ok=True)
            print(rf"Tensorflow / Tensorboard log: {tensorboard_log}")

            if self.launch_tensorboard:
                from tensorboard import program
                tb = program.TensorBoard()
                tb.configure(argv=[None, '--logdir', tensorboard_log])
                url = tb.launch()
                print(f"Tensorflow / Tensorboard listening on url {url}")

        if learning_rate_decrease:
            if self.USE_JAX:
                print("WARNING: linear_schedule not implemented in JAX")
                learning_rate_schedule = learning_rate
            else:
                learning_rate_schedule = self.linear_schedule(learning_rate)
        else:
            learning_rate_schedule = learning_rate

        if CoreBaselines3.USE_JAX:
            from sbx import DQN, PPO, SAC  # sbx-rl
        else:
            from stable_baselines3 import DQN, PPO, SAC

        policy_kwargs = None
        nn_hidden_layers = self.parameters.get(RlAlgorithmParameters.nn_hidden_layers, -1)
        nn_hidden_nodes_multiplier = self.parameters.get(RlAlgorithmParameters.nn_hidden_nodes_multiplier, -1)
        nn_activation_fn = self.parameters.get(RlAlgorithmParameters.nn_activation_fn, None)
        if nn_activation_fn is not None:
            nn_activation_fn = nn_activation_fn.lower()
        custom_nn = nn_hidden_layers > 0 and nn_hidden_layers > 0 and nn_activation_fn is not None and nn_activation_fn in self.ActivationFunction

        if custom_nn:
            policy_kwargs = self._get_custom_policy_kwargs(env, model=self._base_model)

        if self._base_model == PPO:
            model_args = {
                "policy": policy,
                "env": env,
                "verbose": RLAlgorithm.VERBOSE_MODEL,
                "learning_rate": learning_rate_schedule,  # 3e-4
                "seed": seed,
                "gamma": discount_factor,  # 0.99
                "batch_size": batch_size,  # 64
                "n_epochs": epoch,  # 10
                "tensorboard_log": tensorboard_log,
                "device": device,
                "vf_coef": vf_coef,
                "ent_coef": ent_coef,
                "clip_range": clip_range,  # 0.2
                "gae_lambda": gae_lambda,  # 0.95
                "policy_kwargs": policy_kwargs,

            }
            self._add_core_model_kwargs(model_args)

            print(f'PPO : {model_args}')
            model = PPO(**model_args)

        elif self._base_model == DQN or self._base_model == SAC:
            training_predict_iteration_period = self.parameters.get(
                RlAlgorithmParameters.training_predict_iteration_period,
                IterationsPeriodTime.END_OF_SESSION,
            )
            train_freq = self._get_steps_from_iteration_period(
                iteration_period=training_predict_iteration_period
            )
            if not training:
                # disable stats too
                self.parameters[RlAlgorithmParameters.training_stats] = 0
                train_freq = (1, 'episode')
                print(
                    f'testing detected -> train_freq = (1,episode) and explore_pro = {explore_prob}'
                )

            target_update_interval = (
                10000  # timesteps between updates of the target network
            )
            if (
                    RlAlgorithmParameters.training_target_iteration_period
                    in self.parameters
            ):
                target_iteration_period = self.parameters.get(
                    RlAlgorithmParameters.training_target_iteration_period,
                    IterationsPeriodTime.END_OF_SESSION,
                )
                target_update_interval = self._get_steps_from_iteration_period(
                    iteration_period=target_iteration_period
                )
                if target_update_interval[1] == 'step':
                    target_update_interval = target_update_interval[0]
                else:
                    target_update_interval = (
                            train_freq[0] * 2
                    )  # update every 2 train_freq
            if self._base_model == DQN:
                model_args = {
                    "policy": policy,
                    "env": env,
                    "verbose": RLAlgorithm.VERBOSE_MODEL,
                    "learning_rate": learning_rate_schedule,
                    "buffer_size": buffer_size,  # 1_000_000
                    "seed": seed,
                    "gamma": discount_factor,  # 0.99
                    "exploration_initial_eps": 1.0,
                    "exploration_final_eps": explore_prob,  # 0.05
                    "batch_size": batch_size,  # 32
                    "tensorboard_log": tensorboard_log,
                    "train_freq": train_freq,  # 4
                    "target_update_interval": target_update_interval,  # 10000
                    "device": device,
                    "policy_kwargs": policy_kwargs,
                }
                print(
                    f"train_freq[{training_predict_iteration_period}] update the model: {train_freq[0]} {train_freq[1]}",
                )
                print(
                    f"target_update_interval update the model: {target_update_interval} step"
                )
                self._add_core_model_kwargs(model_args)
                print(f'DQN : {model_args}')
                model = DQN(**model_args)

            elif self._base_model == SAC:

                model_args = {
                    "policy": policy,
                    "env": env,
                    "verbose": RLAlgorithm.VERBOSE_MODEL,
                    "learning_rate": learning_rate_schedule,  # 3e-4
                    "buffer_size": buffer_size,  # 1000000
                    "seed": seed,
                    "gamma": discount_factor,  # 0.99
                    "batch_size": batch_size,  # 256
                    "tensorboard_log": tensorboard_log,
                    "train_freq": train_freq,  # 1
                    "device": device,
                    "ent_coef": ent_coef,
                    "policy_kwargs": policy_kwargs,
                }
                print(
                    f"train_freq[{self.parameters[RlAlgorithmParameters.training_predict_iteration_period]}] update the model: {train_freq[0]} {train_freq[1]}",
                )

                self._add_core_model_kwargs(model_args)
                print(f'SAC : {model_args}')
                from gym_zmq.zmq_env_manager import ZmqEnvManager

                model = SAC(**model_args)

        else:
            print(
                rf"Unknown base_model: {self._base_model} -> add implementation to rl_algorithm.py(_create_model_agent)"
            )
            raise NotImplementedError
        # print(rf"RL algorithm: {str(self.base_model)} {policy} {model} created")
        # tensorboard --logdir ./tensorboard/
        # model = self.base_model('MlpPolicy', env, verbose=1, seed=seed)
        return model

    def _create_env(self, env_config, training: bool = False, simultaneous_algos=1):
        from trading_algorithms.reinforcement_learning.env_maker import EnvMaker

        if simultaneous_algos is None or simultaneous_algos == 0:
            simultaneous_algos = 1
        if simultaneous_algos < 1:
            # get the number of processors minus simultaneous_algos
            simultaneous_algos = os.cpu_count() - simultaneous_algos
        simultaneous_algos = min(simultaneous_algos, os.cpu_count())
        # Sequential
        if not self._parallelized_training_model():
            # DQN does not support parallel training
            print(
                rf"base_model {self.base_model_str} detected as not parallelized -> force simultaneous_algos = 1 (DummyVecEnv)"
            )
            simultaneous_algos = 1

        if simultaneous_algos == 1:
            env_configs = [EnvMaker(env_config, counter=i) for i in range(1)]
            env = DummyVecEnv([env_config.create for env_config in env_configs])
        else:
            # Parallel
            print(rf"Starting {simultaneous_algos} parallel environments")
            env_configs = [
                EnvMaker(env_config, counter=i) for i in range(simultaneous_algos)
            ]
            # multiprocessing.get_all_start_methods() # ['fork', 'forkserver', 'spawn']
            env = SubprocVecEnv(
                [env_config.create for env_config in env_configs], start_method=None
            )

        if self.normalize_clip_obs > 0:
            normalizer_env_path = self.rl_paths.normalizer_model_path
            if os.path.isfile(normalizer_env_path):
                print(f"Loading normalize training: {training} model from {normalizer_env_path}...")
                env = VecNormalize.load(normalizer_env_path, env)
                env.training = training

            else:
                print(f"Creating new normalize training: {training} model {normalizer_env_path}...")
                env = VecNormalize(env, norm_obs=True, norm_reward=False, training=training,
                                   clip_obs=self.normalize_clip_obs,
                                   clip_reward=50.0,
                                   gamma=0.95)  # with or without VecNormalize

        return env

    def _load_env(
            self, env0, normalizer_model_path: str = None, training: bool = False
    ):
        if normalizer_model_path is not None:
            if os.path.isfile(normalizer_model_path):
                print(rf"Loading normalize model from {normalizer_model_path} ...")
                env = VecNormalize.load(normalizer_model_path, env0)
                env.training = training
                return env

        print(rf"normalize model not found from {normalizer_model_path} ...")
        return env0

    def check_model(self, model: BaseAlgorithm, env: VecEnv) -> bool:
        assert isinstance(model, BaseAlgorithm)
        assert isinstance(env, VecEnv)
        state_equals = model.observation_space.shape == env.observation_space.shape
        action_equals = model.action_space == env.action_space

        return state_equals and action_equals

    def _load_model(self, env):
        # device = self.parameters.get(RlAlgorithmParameters.device, 'auto')
        return self._base_model.load(self.rl_paths.agent_model_path, env)
    def get_model(self):
        # device = self.parameters.get(RlAlgorithmParameters.device, 'auto')
        return self._base_model.load(self.rl_paths.agent_model_path)

    def save_model(self, env, model, config=None):
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
            RLAlgorithm,
        )
        save_model = True  # save the model in case we dont save it from EvalCallback
        save_env = True  # to save normalization
        save_replay_buffer = True

        if config is not None:
            save_model = config.get(SaveModelConfig.save_model, True)
            save_env = config.get(SaveModelConfig.save_env, True)
            save_replay_buffer = config.get(SaveModelConfig.save_replay_buffer, True)

        if save_model:
            model.save(self.rl_paths.agent_model_path)

        if save_env:
            try:
                env.save(self.rl_paths.normalizer_model_path)
            except Exception as e:
                pass
                # print(rf"WARNING saving normalizer model {e}")
        if save_replay_buffer:
            from stable_baselines3.common.off_policy_algorithm import OffPolicyAlgorithm
            if isinstance(model, OffPolicyAlgorithm):
                try:
                    from configuration import LAMBDA_OUTPUT_PATH
                    complete_path = rf"{self.rl_paths.base_path}/replay_buffer.pkl"
                    model.save_replay_buffer(complete_path)
                    print(rf"Saved replay buffer to {complete_path}")
                except:
                    pass

    def _get_eval_callback(self, env_config, total_total_timesteps_episode, n_eval_episodes=1,
                           start_date_validation: datetime.datetime = None,
                           end_date_validation: datetime.datetime = None) -> 'EvalCallback':
        # EVAL CALLBACK
        eval_env_config = env_config.copy()
        eval_env_config['iterations'] = n_eval_episodes
        from trading_algorithms.reinforcement_learning.env_maker import EnvMaker
        rl_host, rl_port = EnvMaker.get_rl_config(eval_env_config)
        rl_port = rl_port + 1
        eval_env_config = EnvMaker.set_eval_env_config(eval_env_config, rl_host, rl_port, start_date_validation,
                                                       end_date_validation)
        # TODO: check if we need to load env in case of normalizer -> we are training it again
        eval_env0 = self._create_env(eval_env_config, training=True, simultaneous_algos=1)
        eval_save_custom = EvalSaveCustom(eval_env0, self.rl_paths)
        evaluation_callback = EvalCallback(eval_env0, n_eval_episodes=n_eval_episodes,
                                           eval_freq=total_total_timesteps_episode, warn=False,
                                           callback_on_new_best=eval_save_custom,
                                           deterministic=self.DETERMINISTIC
                                           )
        print(
            rf"EvalCallback {rl_port}-> eval_freq: {evaluation_callback.eval_freq}   n_eval_episodes: {evaluation_callback.n_eval_episodes}")

        return evaluation_callback

    def _learn(
            self,
            model,
            env,
            env_config: dict,
            max_episodes,
            patience,
            score_early_stopping,
            min_iterations,
            simultaneous_algos: int = 1,
            tb_log_name: str = None,
            start_date_validation: datetime.datetime = None,
            end_date_validation: datetime.datetime = None,
            validation_callback: bool = True
    ):

        from trading_algorithms.reinforcement_learning.core.baselines3.core_baselines3_callbacks import (
            SaveEndEpisodeInfo,
            EndBacktestEarlyStopping,
            StopTrainingOnMaxEpisodesCustom,
        )
        from trading_algorithms.reinforcement_learning.rl_algorithm import (
            RlAlgorithmParameters,
            RLAlgorithm,
        )

        # setting the learning timesteps
        callback_max_episodes = StopTrainingOnMaxEpisodesCustom(
            max_episodes=max_episodes, verbose=1
        )  # its normalized to number of environments
        callback_each_episode_save = SaveEndEpisodeInfo(verbose=1)
        reward_log = MeanRewardPrintCallback(score_column=InfoStepKey.reward, log_freq_steps_print=1000)
        total_total_timesteps_episode = self._get_number_timesteps_per_episode()

        callbacks = [callback_max_episodes, callback_each_episode_save, reward_log]

        # eval_callback
        if validation_callback:
            eval_callback = self._get_eval_callback(env_config=env_config,
                                                    total_total_timesteps_episode=total_total_timesteps_episode * 3,
                                                    start_date_validation=start_date_validation,
                                                    end_date_validation=end_date_validation)
            print(
                rf"EvalCallback-> eval_freq: {eval_callback.eval_freq}   n_eval_episodes: {eval_callback.n_eval_episodes}")
            callbacks.append(eval_callback)

        if patience is not None and patience > 0:
            if min_iterations < max_episodes:
                # add early stopping
                early_stopping = EndBacktestEarlyStopping(
                    patience=patience,
                    score_column=score_early_stopping,
                    min_iterations=min_iterations,
                    verbose=1,
                )
                callbacks.append(early_stopping)

        # total_timesteps = self.MAX_INT

        episodes = max_episodes
        total_timesteps = total_total_timesteps_episode * episodes
        training_stats_disable = (
                self.parameters.get(RlAlgorithmParameters.training_stats, 0) == 0
        )

        if training_stats_disable:
            tb_log_name = None
        else:
            if tb_log_name is None:
                tb_log_name = self.algorithm_info

        try:
            model.learn(
                total_timesteps=total_timesteps,
                callback=callbacks,
                tb_log_name=tb_log_name,
            )
        except Exception as e:
            print(rf"ERROR training {self.algorithm_info} close env {e}")
            import traceback

            traceback.print_exc()
            if env is not None:
                env.close()
            raise e
        all_infos = callback_each_episode_save.infos_end_episode
        return model, all_infos
