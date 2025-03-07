package com.lambda.investing.algorithmic_trading.gui.algorithm;

import com.lambda.investing.ArrayUtils;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;

import javax.swing.table.AbstractTableModel;

import java.util.List;

import static com.lambda.investing.ArrayUtils.ArrayReverse;
import static com.lambda.investing.model.trading.ExecutionReport.liveStatus;
import static com.lambda.investing.model.trading.ExecutionReport.removedStatus;

@Getter
public class DepthTableModel extends AbstractTableModel {

    private double[][] data = new double[0][0];

    private int askLength = 0;
    private int bidLength = 0;
    private int firstBidRow = 0;

    private static final int BID_QUOTING = 0;
    private static final int ASK_COLUMN = 3;
    private static final int ASK_VOLUME_COLUMN = 4;
    private static final int ASK_QUOTING = 5;

    private static final int BID_COLUMN = 2;
    private static final int BID_VOL_COLUMN = 1;

    private static final int TOTAL_COLUMNS = ASK_QUOTING + 1;

    private static final double DELTA_PRICE_TO_MARK_QUOTING = 0.0001;
    private static final double VOLUME_FACTOR = 1000000;


    private double lastQuoteBidVol = -1;
    private double lastQuoteBid = -1;
    private double lastQuoteAskVol = -1;
    private double lastQuoteAsk = -1;

    private final Object depthLock = new Object();

    public void setData(double[][] data, int askLength, int bidLength) {
        this.data = data;
        this.askLength = askLength;
        this.bidLength = bidLength;
        fireTableDataChanged();
    }

    private void extendBidSide(Depth depth) {
        Double[] bids = depth.getBids();
        Double[] newBids = new Double[bids.length + 1];
        System.arraycopy(bids, 0, newBids, 0, bids.length);
        newBids[bids.length] = null;
        depth.setBids(newBids);

        Double[] bidsVol = depth.getBidsQuantities();
        Double[] newBidsVol = new Double[bidsVol.length + 1];
        System.arraycopy(bidsVol, 0, newBidsVol, 0, bidsVol.length);
        newBidsVol[bidsVol.length] = null;
        depth.setBidsQuantities(newBidsVol);
    }

    private void extendAskSide(Depth depth) {
        Double[] asks = depth.getAsks();
        Double[] newasks = new Double[asks.length + 1];
        System.arraycopy(asks, 0, newasks, 0, asks.length);
        newasks[asks.length] = null;
        depth.setAsks(newasks);

        Double[] asksVol = depth.getAsksQuantities();
        Double[] newasksVol = new Double[asksVol.length + 1];
        System.arraycopy(asksVol, 0, newasksVol, 0, asksVol.length);
        newasksVol[asksVol.length] = null;
        depth.setAsksQuantities(newasksVol);
    }


    public void updateDepth(Depth depth) {
        synchronized (depthLock) {
            Double[] asks = depth.getAsks();
            Double[] bids = depth.getBids();
            double[][] data = new double[asks.length > bids.length ? asks.length * 2 + 1 : bids.length * 2 + 1][TOTAL_COLUMNS];//last row if to mark quoted row
            int bidsLength = bids.length;
            int asksLength = asks.length;
            if (lastQuoteBid != -1 && bidsLength == 1) {
                //one row per side => extend it to have space for the algo quoting
                bidsLength++;
                data = new double[data.length + 1][TOTAL_COLUMNS];

                extendBidSide(depth);
            }

            if (lastQuoteAsk != -1 && asksLength == 1) {
                //one row per side => extend it to have space for the algo quoting
                asksLength++;
                data = new double[data.length + 1][TOTAL_COLUMNS];

                extendAskSide(depth);
            }

            updateAsk(depth, data, asksLength);
            updateBid(depth, data, bidsLength);
            setData(data, asksLength, bidsLength);
        }
    }

    private void updateAsk(Depth depth, double[][] data, int askLength) {
        Double[] asks = ArrayReverse(depth.getAsks());
        Double[] askVols = ArrayReverse(depth.getAsksQuantities());
        Double[] bids = depth.getBids();

        int levelToSetAskQuoting = -1;
        if (lastQuoteAsk != -1) {
            if (lastQuoteAsk < depth.getBestAsk()) {
                //we are the best!
                levelToSetAskQuoting = asks.length - 1;
            }
            if (lastQuoteAsk > depth.getWorstAsk()) {
                //we are the worst
                levelToSetAskQuoting = 0;
            }
        }

        for (int i = 0; i < askLength; i++) {
//                if(asks[i]==null){
//                    data[i][ASK_VOLUME_COLUMN] = 0;
//                    data[i][ASK_COLUMN] = 0;
//                    data[i][ASK_QUOTING] = 0;
//                    continue;
//                }
            if (asks[i] != null) {
                data[i][ASK_VOLUME_COLUMN] = askVols[i] / VOLUME_FACTOR;
                data[i][ASK_COLUMN] = asks[i];
                //compare with lastER
                if (lastQuoteAsk != -1 && levelToSetAskQuoting == -1) {
                    boolean isSamePrice = Math.abs(lastQuoteAsk - asks[i]) < DELTA_PRICE_TO_MARK_QUOTING;
                    boolean canCheckNextLevel = i + 1 < asks.length && asks[i + 1] != null;
                    if (!isSamePrice && canCheckNextLevel) {
                        double nextPrice = asks[i + 1];
                        double currentPrice = asks[i];

                        if (nextPrice < lastQuoteAsk && currentPrice > lastQuoteAsk) {
                            isSamePrice = true;
                        }
                    }

                    if (!isSamePrice && i == asks.length - 1) {
                        //we didn't found position -> we are the worst
                        levelToSetAskQuoting = 0;
                    }
                    if (isSamePrice) {
                        levelToSetAskQuoting = i;
                    }
                }
            }

            if (levelToSetAskQuoting != -1) {
                data[levelToSetAskQuoting][ASK_COLUMN] = lastQuoteAsk;
                data[levelToSetAskQuoting][ASK_QUOTING] = 1;
                data[levelToSetAskQuoting][ASK_VOLUME_COLUMN] += lastQuoteAskVol;
            }

        }
    }

    private void updateBid(Depth depth, double[][] data, int bidLength) {
        Double[] asks = depth.getAsks();
        Double[] bids = depth.getBids();
        Double[] bidVols = depth.getBidsQuantities();
        firstBidRow = asks.length;

        int levelToSetBidQuoting = -1;
        if (lastQuoteBid != -1) {
            if (lastQuoteBid > depth.getBestBid()) {
                //we are the best!
                levelToSetBidQuoting = firstBidRow;
            }
            if (lastQuoteBid < depth.getWorstBid()) {
                //we are the worst
                levelToSetBidQuoting = firstBidRow + bids.length - 1;
            }
        }

        for (int i = 0; i < bidLength; i++) {
            int indexWrite = firstBidRow + i;
            if (bids[i] != null) {
                data[indexWrite][BID_VOL_COLUMN] = bidVols[i] / VOLUME_FACTOR;
                data[indexWrite][BID_COLUMN] = bids[i];

                //compare with lastER
                if (lastQuoteBid != -1 && levelToSetBidQuoting == -1) {
                    boolean isSamePrice = Math.abs(lastQuoteBid - bids[i]) < DELTA_PRICE_TO_MARK_QUOTING;
                    boolean canCheckNextLevel = i + 1 < bids.length && bids[i + 1] != null;
                    if (!isSamePrice && canCheckNextLevel) {
                        double currentPrice = bids[i];
                        double nextPrice = bids[i + 1];

                        if (nextPrice < lastQuoteBid && currentPrice > lastQuoteBid) {
                            isSamePrice = true;
                        }
                    }

                    if (!isSamePrice && i == bids.length - 1) {
                        //we didn't found position -> we are the worst
                        levelToSetBidQuoting = firstBidRow;
                    }

                    if (isSamePrice) {
                        levelToSetBidQuoting = indexWrite;
                    }
                }

            }

        }
        if (levelToSetBidQuoting != -1) {
            data[levelToSetBidQuoting][BID_COLUMN] = lastQuoteBid;
            data[levelToSetBidQuoting][BID_QUOTING] = 1;
            data[levelToSetBidQuoting][BID_VOL_COLUMN] += lastQuoteBidVol;
        }


    }

    public void updateExecutionReport(ExecutionReport executionReport) {
        //in reality is updated next depth -> happens inmediatelly
        if (liveStatus.contains(executionReport.getExecutionReportStatus())) {
            if (executionReport.getVerb() == Verb.Buy) {
                lastQuoteBidVol = executionReport.getQuantity() / VOLUME_FACTOR;
                lastQuoteBid = executionReport.getPrice();
            } else {
                lastQuoteAskVol = executionReport.getQuantity() / VOLUME_FACTOR;
                lastQuoteAsk = executionReport.getPrice();
            }
        }

        if (removedStatus.contains(executionReport.getExecutionReportStatus())) {
            if (executionReport.getVerb() == Verb.Buy) {
                lastQuoteBidVol = -1;
                lastQuoteBid = -1;
            } else {
                lastQuoteAskVol = -1;
                lastQuoteAsk = -1;
            }
        }
    }

    public int getRowCount() {
        return data.length;
    }

    public int getColumnCount() {
        return TOTAL_COLUMNS;
    }

    public Object getValueAt(int row, int column) {
        if (row >= data.length || data[row][column] == 0.0) {
            return "";
        } else {
            return Double.toString(data[row][column]);
        }
    }

    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return "Algo";
            case 1:
                return "Vol";
            case 2:
                return "Bid";
            case 3:
                return "Ask";
            case 4:
                return "Vol";
            case 5:
                return "Algo";
            default:
                return "";
        }
    }
}