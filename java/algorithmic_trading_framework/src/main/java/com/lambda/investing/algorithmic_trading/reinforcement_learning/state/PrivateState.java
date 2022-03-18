package com.lambda.investing.algorithmic_trading.reinforcement_learning.state;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.MatrixRoundUtils;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreEnum;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.ScoreUtils;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import lombok.Getter;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static com.lambda.investing.algorithmic_trading.reinforcement_learning.MatrixRoundUtils.getDiffQueue;
import static com.lambda.investing.algorithmic_trading.reinforcement_learning.MatrixRoundUtils.getValuesPerColumn;

/***
 *
 * State based on current position and past scores diff rounded by number_decimals_round_state
 *
 */
@Getter public class PrivateState extends AbstractState {

	private static boolean QUANTITY_RELATIVE = true;
	private static boolean DELTA_STATES = false;

	private static String[] COLUMNS_PATTERN = new String[] { "inventory", "score" };

	private ScoreEnum scoreEnumColumn;
	private int horizonSave;

	private long tickMs;

	private long lastTickSave = 0;
	private Queue<Double> inventoryBuffer;
	private Queue<Double> scoreBuffer;
	private double quantity;

	public PrivateState(ScoreEnum scoreEnum, int numberOfDecimals, int horizonSave, long tickMs, double maxNumber,
			double minNumber, double quantity) {
		super(numberOfDecimals);
		this.quantity = quantity;
		this.numberOfDecimals = numberOfDecimals;
		this.scoreEnumColumn = scoreEnum;
		this.horizonSave = horizonSave;
		this.tickMs = tickMs;
		this.numberOfColumns = getColumns().size();
		if (minNumber >= maxNumber) {
			logger.warn("privateState minNumber {} maxNumber {} is wrong -> set default ", minNumber, maxNumber);
			maxNumber = MAX_NUMBER;
			minNumber = MIN_NUMBER;
		}
		this.maxNumber = maxNumber;
		this.minNumber = minNumber;
		this.valuesPerColum = getValuesPerColumn(this.numberOfDecimals, this.maxNumber, this.minNumber);

		//save one more to diff it
		if (DELTA_STATES) {
			inventoryBuffer = EvictingQueue.create(this.horizonSave + 1);
			scoreBuffer = EvictingQueue.create(this.horizonSave + 1);
		} else {
			inventoryBuffer = EvictingQueue.create(this.horizonSave);
			scoreBuffer = EvictingQueue.create(this.horizonSave);
		}

	}

	@Override public List<String> getColumns() {
		List<String> out = new ArrayList<>();
		for (int horizonTick = 0; horizonTick < horizonSave; horizonTick++) {
			for (int colIndex = 0; colIndex < COLUMNS_PATTERN.length; colIndex++) {
				out.add(COLUMNS_PATTERN[colIndex] + "_" + String.valueOf(horizonTick));
			}
		}
		return out;
	}

	@Override public int getNumberStates() {
		return MatrixRoundUtils.getNumberStates(valuesPerColum, horizonSave * COLUMNS_PATTERN.length);
	}

	@Override public boolean isReady() {

		if (DELTA_STATES) {
			if (this.inventoryBuffer.size() < this.horizonSave + 1 || this.scoreBuffer.size() < this.horizonSave + 1) {
				//				logger.error("not enough states received {}< {} ", this.inventoryBuffer.size(), this.horizonSave + 1);
				return false;
			}
		} else {
			if (this.inventoryBuffer.size() < this.horizonSave || this.scoreBuffer.size() < this.horizonSave) {
				//				logger.error("not enough states received {}< {} ", this.inventoryBuffer.size(), this.horizonSave);
				return false;
			}
		}
		return true;

	}

	@Override public int getStateFromArray(double[] state) {
		assert state.length == this.numberOfColumns;
		return super.getStateFromArray(state);
	}

	@Override protected double[] getState(int statePosition) {
		return super.getState(statePosition);
	}

	@Override public void updateCandle(Candle candle) {

	}

	@Override public void updateTrade(Trade trade) {

	}

	public void updatePrivateState(PnlSnapshot pnlSnapshot) {
		if ((pnlSnapshot.getLastTimestampUpdate() - lastTickSave) < tickMs) {
			//not enough time to save it
			return;
		}

		double score = ScoreUtils.getReward(this.scoreEnumColumn, pnlSnapshot);
		if (QUANTITY_RELATIVE) {
			score = score / quantity;
		}
		scoreBuffer.add(score);

		double position = pnlSnapshot.netPosition;
		if (QUANTITY_RELATIVE) {
			position = position / quantity;
		}
		inventoryBuffer.add(position);

		lastTickSave = pnlSnapshot.getLastTimestampUpdate();
	}

	@Override public void updateDepthState(Depth depth) {

	}

	@Override public double[] getCurrentState() {
		if (!isReady()) {
			logger.error("not enough states received {}< {} ", this.inventoryBuffer.size(), this.horizonSave + 1);
			return null;
		}

		List<Double> outputList = new ArrayList<>(this.numberOfColumns);

		if (DELTA_STATES) {
			outputList.addAll(getDiffQueue(inventoryBuffer));
			outputList.addAll(getDiffQueue(scoreBuffer));
		} else {
			outputList.addAll(inventoryBuffer);
			outputList.addAll(scoreBuffer);
		}

		double[] outputArr = outputList.stream().mapToDouble(Double::doubleValue).toArray();
		outputArr = getFilteredState(outputArr);//get filter
		return outputArr;
	}

	@Override public int getCurrentStatePosition() {
		double[] currentStateArr = getCurrentState();//filtered
		return getStateFromArray(currentStateArr);
	}

	@Override public void enumerateStates(String cachePermutationsPath) {
		fillCache(cachePermutationsPath);
	}

}
