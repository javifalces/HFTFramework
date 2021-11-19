class AlgorithmEnum:
    avellaneda_q = "AvellanedaQ"
    avellaneda_dqn = "AvellanedaDQN"
    avellaneda_stoikov = "AvellanedaStoikov"
    constant_spread = "ConstantSpread"
    linear_constant_spread = "LinearConstantSpread"


def get_algorithm(algorithm_enum: AlgorithmEnum):
    from backtest.avellaneda_q import AvellanedaQ
    from backtest.avellaneda_stoikov import AvellanedaStoikov
    from backtest.avellaneda_dqn import AvellanedaDQN
    from backtest.constant_spread import ConstantSpread
    from backtest.linear_constant_spread import LinearConstantSpread

    if algorithm_enum == AlgorithmEnum.avellaneda_q:
        return AvellanedaQ
    if algorithm_enum == AlgorithmEnum.avellaneda_stoikov:
        return AvellanedaStoikov
    if algorithm_enum == AlgorithmEnum.avellaneda_dqn:
        return AvellanedaDQN
    if algorithm_enum == AlgorithmEnum.constant_spread:
        return ConstantSpread
    if algorithm_enum == AlgorithmEnum.linear_constant_spread:
        return LinearConstantSpread