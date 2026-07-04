package com.lambda.investing.algorithmic_trading.pnl_calculation;

import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;


@Getter
public class PortfolioSnapshot {
    protected Map<String, PnlSnapshot> instrumentPnlSnapshotMap;
    public double netInvestment, realizedPnl, unrealizedPnl, totalPnl, totalFees, realizedFees, unrealizedFees, netPosition;
    public String algorithmInfo;

    @Setter
    public long lastTimestampUpdate;

    public PortfolioSnapshot(String algorithmInfo, Map<String, PnlSnapshot> instrumentPnlSnapshotMap) {
        this.algorithmInfo = algorithmInfo;
        this.instrumentPnlSnapshotMap = instrumentPnlSnapshotMap;
        calculatePortfolioSnapshot();
    }

    public PnlSnapshot getPnlSnapshot(String instrument) {
        return instrumentPnlSnapshotMap.get(instrument);
    }


    public Map<String, PnlSnapshot> getPnlSnapshots() {
        return instrumentPnlSnapshotMap;
    }

    private void calculatePortfolioSnapshot() {
        //calculate all doubles from instrumentPnlSnapshotMap sum them up
        netInvestment = 0;
        realizedPnl = 0;
        unrealizedPnl = 0;
        totalPnl = 0;
        totalFees = 0;
        realizedFees = 0;
        unrealizedFees = 0;
        lastTimestampUpdate = 0;
        netPosition = 0;
        for (PnlSnapshot pnlSnapshot : instrumentPnlSnapshotMap.values()) {
            netInvestment += pnlSnapshot.netInvestment;
            realizedPnl += pnlSnapshot.realizedPnl;
            unrealizedPnl += pnlSnapshot.unrealizedPnl;
            totalPnl += pnlSnapshot.totalPnl;
            totalFees += pnlSnapshot.totalFees;
            realizedFees += pnlSnapshot.realizedFees;
            unrealizedFees += pnlSnapshot.unrealizedFees;
            netPosition += pnlSnapshot.netPosition;
            lastTimestampUpdate = Math.max(lastTimestampUpdate, pnlSnapshot.lastTimestampUpdate);
        }
    }

    public double getReward(ScoreEnum scoreEnum) {
        double output = 0.0;
        if (scoreEnum.equals(ScoreEnum.realized_pnl)) {
            output = realizedPnl;
        } else if (scoreEnum.equals(ScoreEnum.total_pnl)) {
            output = totalPnl;
        } else if (scoreEnum.equals(ScoreEnum.asymmetric_dampened_pnl)) {
            double speculative = Math.min(unrealizedPnl, 0.);
            output = (realizedPnl + speculative);
        } else if (scoreEnum.equals(ScoreEnum.unrealized_pnl)) {
            output = (unrealizedPnl);
        } else if (scoreEnum.equals(ScoreEnum.pnl_to_map)) {
            output = (totalPnl / Math.abs(netInvestment));
        } else if (scoreEnum.equals(ScoreEnum.asymmetric_dampened_pnl_to_map)) {
            double speculative = Math.min(unrealizedPnl, 0.);
            double asymmetric = (realizedPnl + speculative);
            output = (asymmetric / (1 + Math.abs(netInvestment)));
        } else {
            System.out.println(scoreEnum + " not found to calculate score");
            output = -1;
        }
        if (Double.isNaN(output)) {
            System.out.println("reward output detected as Nan-> return 0.0");
            output = 0.0;
        }

        return output;
    }


    @Override
    public String toString() {
        StringBuffer output = new StringBuffer();
        for (Map.Entry<String, PnlSnapshot> entry : instrumentPnlSnapshotMap.entrySet()) {
            output.append("----").append("\n");
            output.append("Instrument: ").append(entry.getKey()).append("\n");
            String pnlDump = entry.getValue().dumpString();
            output.append(pnlDump).append("\n");
            output.append("----").append("\n");
        }
        return output.toString();
    }
}
