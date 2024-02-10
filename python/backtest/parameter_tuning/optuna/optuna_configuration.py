from optuna.samplers import RandomSampler, TPESampler, CmaEsSampler, GridSampler, intersection_search_space \
    , NSGAIISampler, NSGAIIISampler


from trading_algorithms.reinforcement_learning.rl_algorithm import InfoStepKey


class OptunaConfiguration:
    n_trials = 100
    score_column = (
        InfoStepKey.cumDayReward
    )  #
    n_jobs = 1
    sampler = RandomSampler
    direction = 'maximize'
    sampler_kwargs = None
