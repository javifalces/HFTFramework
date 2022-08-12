from trading_algorithms.score_enum import ScoreEnum


class GAConfiguration:
    equity_column_score = ScoreEnum.realized_pnl  # can be realized_pnl or total_pnl
    elite_population_percent = 0.1
    population = 5
    crossover_prob = (
        0.7  # 0.7 means 70pct possiblities to have crossover on next generation
    )
    sigma = 2
    decay = 0.1
    exp_multiplier = 0.5

    score_column = (
        ScoreEnum.sharpe
    )  # can be ,asymmetric_dampened_pnl ,open_pnl ,close_pnl ,sharpe

    def __str__(self):
        return (
            'GAConfiguration:\n'
            '\telite_population_percent: %.2f\n'
            '\tcrossover_prob: %.2f\n'
            '\tpopulation: %.2f\n'
            '\tsigma: %.2f\n'
            '\tdecay: %.3f\n'
            '\tscore_column: %s\n'
            '\tcolumn_score_applied: %s\n'
            % (
                self.elite_population_percent,
                self.crossover_prob,
                self.population,
                self.sigma,
                self.decay,
                self.score_column,
                self.equity_column_score,
            )
        )
