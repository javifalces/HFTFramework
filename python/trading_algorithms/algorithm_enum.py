class AlgorithmEnum:
    avellaneda_stoikov = "AvellanedaStoikov"
    rl4j_alpha_avellaneda_stoikov = "RL4jAlphaAvellanedaStoikov"
    alpha_avellaneda_stoikov = "AlphaAvellanedaStoikov"
    alpha_constant_spread = "AlphaConstantSpread"
    constant_spread = "ConstantSpread"
    linear_constant_spread = "LinearConstantSpread"




def get_algorithm(algorithm_enum: AlgorithmEnum):
    from trading_algorithms.market_making.avellaneda_stoikov import AvellanedaStoikov

    from trading_algorithms.market_making.constant_spread import ConstantSpread
    from trading_algorithms.market_making.linear_constant_spread import (
        LinearConstantSpread,
    )
    from trading_algorithms.market_making.alpha_avellaneda_stoikov import (
        AlphaAvellanedaStoikov,
    )
    from trading_algorithms.market_making.alpha_constant_spread import (
        AlphaConstantSpread,
    )

    if algorithm_enum == AlgorithmEnum.avellaneda_stoikov:
        return AvellanedaStoikov
    if algorithm_enum == AlgorithmEnum.constant_spread:
        return ConstantSpread
    if algorithm_enum == AlgorithmEnum.linear_constant_spread:
        return LinearConstantSpread
    if algorithm_enum == AlgorithmEnum.alpha_avellaneda_stoikov:
        return AlphaAvellanedaStoikov
    if algorithm_enum == AlgorithmEnum.alpha_constant_spread:
        return AlphaConstantSpread

