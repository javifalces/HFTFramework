from typing import Dict, Any, Optional

from stable_baselines3.common.callbacks import (
    StopTrainingOnMaxEpisodes,
    BaseCallback,
    StopTrainingOnNoModelImprovement,
    EvalCallback,
)
import datetime
import numpy as np
import time
import pandas as pd
from trading_algorithms.iterations_period_time import IterationsPeriodTime
from trading_algorithms.reinforcement_learning.rl_algorithm import (
    InfoStepKey,
)


class SaveEndEpisodeInfo(BaseCallback):
    def __init__(self, verbose: int = 0):
        super().__init__(verbose=verbose)
        self.n_episodes = 0
        self.infos_end_episode = []

    def _on_step(self) -> bool:
        assert (
                "dones" in self.locals
        ), "`dones` variable is not defined, please check your code next to `callback.on_step()`"
        n_episodes = self.n_episodes + np.sum(self.locals["dones"]).item()
        if n_episodes > self.n_episodes:
            # new episode detected
            if "infos" in self.locals:
                self.infos_end_episode.append(self.locals["infos"][0])
            self.n_episodes = n_episodes
        return True


class EndBacktestEarlyStopping(BaseCallback):
    def __init__(
            self,
            patience,
            min_iterations: int = 1,
            verbose: int = 0,
            score_column: str = InfoStepKey.totalPnl,
    ):
        '''

        Parameters
        ----------
        patience
        min_iterations
        verbose
        score_column : totalPnl , realizedPnl , unrealizedPnl, cumDayReward
        '''
        super().__init__(verbose=verbose)
        self.rewards = []
        self.last_reward = 0
        self.min_iterations = min_iterations

        self.patience = patience
        self.counter_patience = 0
        self.score_column = score_column
        self.n_episodes = 0

        self._min_iterations = self.min_iterations
        self._patience = self.patience

    # def _init_callback(self) -> None:
    #     # At start set total max according to number of envirnments
    #     self._min_iterations = self.min_iterations * self.training_env.num_envs
    #     self._patience = self.patience * self.training_env.num_envs

    def _save_reward(self):
        last_reward = self.locals["rewards"][0]
        if "infos" in self.locals:
            try:
                last_info = self.locals["infos"][0]
                last_reward = last_info[self.score_column]
            except:
                print(
                    rf"WARNING: EndBacktestEarlyStopping can't find {self.score_column} in last_info -> set as 0.0"
                )
                last_reward = 0.0

        self.last_reward = float(last_reward)
        self.rewards.append(self.last_reward)

    def _check_early_stopping(self) -> bool:
        # get the best reward in the last n iterations
        number_iterations = len(self.rewards)
        iterations_search = min(self._patience, len(self.rewards))
        previous_best_reward = max(self.rewards[-iterations_search:])
        best_reward = max(self.rewards)

        continue_training = True
        if number_iterations < self._min_iterations:
            print(
                rf"EndBacktestEarlyStopping continue training until {number_iterations} >= {self._min_iterations} min_iterations "
            )
            return continue_training

        new_patience_record = self.last_reward == best_reward
        if new_patience_record:
            print(
                rf"EndBacktestEarlyStopping continue training in iteration {number_iterations} because  new high score {self.score_column} = {self.last_reward:.2f}=={previous_best_reward:.2f} >{best_reward:.2f} in {iterations_search} last iterations "
            )
            self.counter_patience = 0
            continue_training = True
            return continue_training

        # previous_best_reward_still_best = previous_best_reward >= best_reward
        # if previous_best_reward_still_best:
        #     self.counter_patience = 0
        #     continue_training = True
        #     print(
        #         rf"EndBacktestEarlyStopping continue training in iteration {number_iterations}, {self.score_column} = {previous_best_reward:.2f} still the best in {iterations_search} last iterations")
        #     return continue_training

        last_is_not_the_best = previous_best_reward < best_reward
        if last_is_not_the_best:
            patience_is_over = self.counter_patience >= self._patience
            if patience_is_over:
                print(
                    rf"EndBacktestEarlyStopping stopping training in iteration {number_iterations} because  score {self.score_column} is not better and patience is over,   {previous_best_reward:.2f}<{best_reward:.2f} in {iterations_search} last iterations  [{self.counter_patience}/{self._patience}]"
                )
                continue_training = False
                self.counter_patience = 0
            else:
                print(
                    rf"EndBacktestEarlyStopping continue training in iteration {number_iterations} because {self.score_column} is not better but patience is not over, {previous_best_reward:.2f}<{best_reward:.2f} in {iterations_search} last iterations [{self.counter_patience}/{self._patience}]"
                )
                self.counter_patience += 1
                continue_training = True

        return continue_training

    def _on_step(self) -> bool:
        assert (
                "dones" in self.locals
        ), "`dones` variable is not defined, please check your code next to `callback.on_step()`"

        continue_training = True
        episode_is_finished = False

        n_episodes = self.n_episodes + np.sum(self.locals["dones"]).item()
        if n_episodes > self.n_episodes:
            episode_is_finished = True
            self.n_episodes = n_episodes

        if episode_is_finished:
            self._save_reward()

        if episode_is_finished:
            continue_training = self._check_early_stopping()

        return continue_training


class StopTrainingOnMaxEpisodesCustom(BaseCallback):
    """
    Stop the training once a maximum number of episodes are played.

    For multiple environments presumes that, the desired behavior is that the agent trains on each env for ``max_episodes``
    and in total for ``max_episodes * n_envs`` episodes.

    :param max_episodes: Maximum number of episodes to stop training.
    :param verbose: Verbosity level: 0 for no output, 1 for indicating information about when training ended by
        reaching ``max_episodes``
    """

    def __init__(self, max_episodes: int, verbose: int = 0):
        super().__init__(verbose=verbose)
        self.max_episodes = max_episodes
        self._total_max_episodes = max_episodes
        self.n_episodes = 0

    # def _init_callback(self) -> None:
    #     # At start set total max according to number of envirnments
    #     # self._total_max_episodes = self.max_episodes * self.training_env.num_envs

    def _on_step(self) -> bool:
        # Check that the `dones` local variable is defined
        assert (
                "dones" in self.locals
        ), "`dones` variable is not defined, please check your code next to `callback.on_step()`"
        self.n_episodes += np.sum(self.locals["dones"]).item()

        continue_training = self.n_episodes < self._total_max_episodes

        if self.verbose >= 1 and not continue_training:
            mean_episodes_per_env = self.n_episodes / self.training_env.num_envs
            mean_ep_str = (
                f"with an average of {mean_episodes_per_env:.2f} episodes per env"
                if self.training_env.num_envs > 1
                else ""
            )

            print(
                f"Stopping training with a total of {self.num_timesteps} steps because the "
                f"{self.locals.get('tb_log_name')} model reached max_episodes={self.max_episodes}, "
                f"by playing for {self.n_episodes} episodes "
                f"{mean_ep_str}"
            )
        return continue_training


class MeanRewardPrintCallback(BaseCallback):
    '''
    Snippet skeleton from Stable baselines3 documentation here:
    https://stable-baselines3.readthedocs.io/en/master/guide/tensorboard.html#directly-accessing-the-summary-writer
    '''

    def __init__(self, verbose: int = 0, log_freq_steps_print: int = 1000, score_column: str = InfoStepKey.reward):
        '''

        Parameters
        ----------
        verbose
        log_freq: log every n steps
        score_column: totalPnl , realizedPnl , unrealizedPnl, cumDayReward
        '''
        super().__init__(verbose)
        self.score_column = score_column
        self.log_freq_steps_print = log_freq_steps_print
        self.all_rewards = []
        self.episode_rewards = []
        self.mean_last_reward = 0.0
        self.last_episode = 0

    def _on_step(self) -> bool:
        '''
        Log my_custom_reward every _log_freq(th) to tensorboard for each environment
        '''
        n_episodes = np.sum(self.locals["dones"]).item()
        if n_episodes > self.last_episode:
            self.episode_rewards = []

        if self.n_calls > 0:
            try:
                reward = self.locals['infos'][0][self.score_column]
                reward = float(reward)
            except Exception as e:
                print(
                    rf"WARNING: MeanRewardPrintCallback can't find {self.score_column} in last_info {e} -> not save reward ")
                reward = None

            if reward is not None and not np.isnan(reward) and not np.isinf(reward):
                self.all_rewards.append(reward)
                self.episode_rewards.append(reward)

            if self.n_calls % self.log_freq_steps_print == 0:
                all_rewards_np = np.array(self.all_rewards)
                episode_rewards_np = np.array(self.episode_rewards)
                mean_reward = all_rewards_np.mean()
                mean_reward_episode = episode_rewards_np.mean()

                zeroes_rewards = np.count_nonzero(np.abs(all_rewards_np) <= 1E-9)
                if self.verbose >= 1:
                    print(
                        rf"{self.score_column} in {self.n_calls} steps  = {mean_reward}  improvement last = {mean_reward - self.mean_last_reward} ")

                self.logger.record("train/mean_reward", mean_reward)
                self.logger.record("train/mean_reward_episode", mean_reward_episode)
                # self.logger.record(f"train/{self.log_freq_steps_print}_steps_mean_reward", mean_reward_last)
                # self.logger.record(f"train/{self.log_freq_steps_print}_steps_mean_reward_improvement",
                #                    mean_reward_last - self.mean_last_reward)
                self.logger.record("train/zero_reward_pct", zeroes_rewards / len(self.all_rewards))

                self.mean_last_reward = mean_reward_episode

        return True


class EvalSaveCustom(BaseCallback):
    def __init__(self, env, rl_paths: 'RlPaths', verbose: int = 0):
        super().__init__(verbose=verbose)
        self.env = env
        self.rl_paths = rl_paths
        self.agent_model_path = self.rl_paths.agent_model_path
        self.normalizer_model_path = self.rl_paths.normalizer_model_path

    def _on_step(self) -> bool:
        '''
        Save model every call
        '''
        print(rf"save model to {self.agent_model_path}")
        self.model.save(self.agent_model_path)
        try:
            self.env.save(self.normalizer_model_path)
            print(rf"save normalizer to {self.normalizer_model_path}")
        except:
            pass

        return True
