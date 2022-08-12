package com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn;

import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.AbstractState;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;

import java.util.ArrayList;
import java.util.List;

public class TestState extends AbstractState {

	public double[] currentState;

	public TestState(int numberOfDecimals, double[] sampleState) {
		super(numberOfDecimals);
		setNumberOfColumns(sampleState.length);
		this.currentState = sampleState;
	}

	@Override public void calculateNumberOfColumns() {
	}

	@Override public List<String> getColumns() {
		List<String> columns = new ArrayList<>();
		for (int i = 0; i < this.currentState.length; i++) {
			columns.add("column_" + i);
		}
		return columns;
	}

	@Override
	public int getNumberStates() {
		return this.currentState.length;
	}

	@Override
	public void reset() {

	}

	@Override
	public boolean isReady() {
		return true;
	}

	public void setCurrentState(double[] stateArr) {
		this.currentState = currentState;
	}

	@Override
	public double[] getCurrentState() {
		return this.currentState;
	}

	@Override public int getCurrentStatePosition() {
		double[] currentStateArr = getCurrentStateRounded();//filtered
		return getStateFromArray(currentStateArr);

	}

	@Override public void enumerateStates(String cachePermutationsPath) {

	}

	@Override public void updateCandle(Candle candle) {

	}

	@Override public void updateTrade(Trade trade) {

	}

	@Override public void updatePrivateState(PnlSnapshot pnlSnapshot) {

	}

	@Override public void updateDepthState(Depth depth) {

	}
}
