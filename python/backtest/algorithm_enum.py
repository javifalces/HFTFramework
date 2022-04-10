


class AlgorithmEnum:
    # avellaneda_q = "AvellanedaQ"
    # avellaneda_dqn = "AvellanedaDQN"
    avellaneda_stoikov = "AvellanedaStoikov"
    alpha_avellaneda_stoikov = "AlphaAvellanedaStoikov"
    alpha_constant_spread = "AlphaConstantSpread"
    constant_spread = "ConstantSpread"
    linear_constant_spread = "LinearConstantSpread"
    # sma_cross = "SMACross"
    # rsi = "RSISideQuoting"
    # stat_arb = "StatArb"
    # rsi_dqn = 'DQNRSISideQuoting'
    # moving_average_dqn = 'DQNMovingAverage'
    # onnx_portfolio = "OnnxPortfolioAlgorithm"
    # arbitrage = "Arbitrage"


def get_algorithm(algorithm_enum: AlgorithmEnum):
    from backtest.avellaneda_q import AvellanedaQ
    from backtest.avellaneda_stoikov import AvellanedaStoikov
    from backtest.avellaneda_dqn import AvellanedaDQN
    # from backtest.sma_cross import SMACross
    # from backtest.rsi import RSI
    # from backtest.stat_arb import StatArb
    # from backtest.arbitrage import Arbitrage
    # from backtest.rsi_dqn import RsiDQN
    from backtest.constant_spread import ConstantSpread
    from backtest.linear_constant_spread import LinearConstantSpread
    from backtest.alpha_avellaneda_stoikov import AlphaAvellanedaStoikov
    from backtest.alpha_constant_spread import AlphaConstantSpread

    if algorithm_enum == AlgorithmEnum.avellaneda_q:
        return AvellanedaQ
    if algorithm_enum == AlgorithmEnum.avellaneda_stoikov:
        return AvellanedaStoikov
    if algorithm_enum == AlgorithmEnum.avellaneda_dqn:
        return AvellanedaDQN
    # if algorithm_enum == AlgorithmEnum.sma_cross:
    #     return SMACross
    # if algorithm_enum == AlgorithmEnum.rsi:
    #     return RSI
    # if algorithm_enum == AlgorithmEnum.stat_arb:
    #     return StatArb
    # if algorithm_enum == AlgorithmEnum.rsi_dqn:
    #     return RsiDQN
    if algorithm_enum == AlgorithmEnum.constant_spread:
        return ConstantSpread
    if algorithm_enum == AlgorithmEnum.linear_constant_spread:
        return LinearConstantSpread

    if algorithm_enum == AlgorithmEnum.alpha_avellaneda_stoikov:
        return AlphaAvellanedaStoikov
    if algorithm_enum == AlgorithmEnum.alpha_constant_spread:
        return AlphaConstantSpread
    # if algorithm_enum == AlgorithmEnum.arbitrage:
    #     return Arbitrage
