package com.lambda.investing.algorithmic_trading.reinforcement_learning.action;

import com.lambda.investing.algorithmic_trading.reinforcement_learning.MatrixRoundUtils;

public class SpreadAction extends AbstractAction {

	private static String[] COLUMNS = new String[] { "bidSpread", "askSpread" };
	private double maxSpread, minSpread;//spread to mid
	private int numberOfDecimals, numberOfPossibleSpreads;

	public SpreadAction(double maxSpread, double minSpread, int numberOfDecimals) {
		this.maxSpread = maxSpread;
		this.minSpread = minSpread;
		this.numberOfDecimals = numberOfDecimals;

		this.numberOfPossibleSpreads = MatrixRoundUtils
				.getValuesPerColumn(this.numberOfDecimals, this.maxSpread, this.minSpread);
	}

	@Override public int getNumberActions() {
		return MatrixRoundUtils.getNumberStates(this.numberOfPossibleSpreads, 2);
	}

	protected int getColumnPosition(int columnIndex, double roundedNumber) {
		double decimalDen = Math.pow(10, numberOfDecimals);
		double value = roundedNumber - Math.floor(minSpread);
		int positionInColumn = (int) Math.round(value / decimalDen);
		return positionInColumn + columnIndex * numberOfPossibleSpreads;
	}

	@Override public int getAction(double[] actionArr) {
		assert actionArr.length == COLUMNS.length;

		double decimalMultiply = 1 / Math.pow(10, (numberOfDecimals));

		int output = 0;
		for (int column = 0; column < COLUMNS.length; column++) {
			double value = Math.round(actionArr[column] / decimalMultiply) * decimalMultiply;
			value = Math.min(maxSpread, value);
			value = Math.max(minSpread, value);
			output += getColumnPosition(column, value);
		}
		return output;
	}

	@Override public double[] getAction(int actionPos) {
		//TODO
		return new double[0];
	}
}
