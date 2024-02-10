package com.lambda.investing.algorithmic_trading.reinforcement_learning;

import com.lambda.investing.algorithmic_trading.Algorithm;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScoreUtils {

	static Logger logger = LogManager.getLogger(Algorithm.class);

	public static double getReward(ScoreEnum scoreEnum, PnlSnapshot pnlSnapshot) {
		double output = 0.0;
		if (scoreEnum.equals(ScoreEnum.realized_pnl)) {
			output = pnlSnapshot.getRealizedPnl();
		} else if (scoreEnum.equals(ScoreEnum.total_pnl)) {
			output = pnlSnapshot.getTotalPnl();
		} else if (scoreEnum.equals(ScoreEnum.asymmetric_dampened_pnl)) {
			double speculative = Math.min(pnlSnapshot.unrealizedPnl, 0.);
			output = (pnlSnapshot.realizedPnl + speculative);
		} else if (scoreEnum.equals(ScoreEnum.unrealized_pnl)) {
			output = (pnlSnapshot.unrealizedPnl);
		} else if (scoreEnum.equals(ScoreEnum.pnl_to_map)) {
			output = (pnlSnapshot.getTotalPnl() / Math.abs(pnlSnapshot.netPosition));
		} else if (scoreEnum.equals(ScoreEnum.asymmetric_dampened_pnl_to_map)) {
			double speculative = Math.min(pnlSnapshot.unrealizedPnl, 0.);
			double asymmetric = (pnlSnapshot.realizedPnl + speculative);
			output = (asymmetric / (1 + Math.abs(pnlSnapshot.netPosition)));
		} else {
			logger.error("{} not found to calculate score", scoreEnum);
			output = -1;
		}
		if (Double.isNaN(output)) {
			logger.warn("reward output detected as Nan-> return 0.0");
			output = 0.0;
		}

		return output;
	}

}
