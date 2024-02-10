class ScoreEnum:
    total_pnl = 'total_pnl'
    realized_pnl = 'realized_pnl'
    unrealized_pnl = 'unrealized_pnl'
    asymmetric_dampened_pnl = 'asymmetric_dampened_pnl'
    pnl_to_map = 'pnl_to_map'
    sharpe = 'sharpe'
    sortino = 'sortino'
    ulcer = 'ulcer'
    max_dd = 'max_dd'
    falcma_ratio = 'falcma_ratio'
    asymmetric_dampened_pnl_to_map = 'asymmetric_dampened_pnl_to_map'
    number_trades = 'number_trades'


def get_score_enum_csv_column(score_enum: ScoreEnum):
    if score_enum is ScoreEnum.total_pnl:
        return 'historicalTotalPnl'
    if score_enum is ScoreEnum.realized_pnl:
        return 'historicalRealizedPnl'
    if score_enum is ScoreEnum.unrealized_pnl:
        return 'historicalUnrealizedPnl'
    if score_enum is ScoreEnum.number_trades:
        return 'numberTrades'
