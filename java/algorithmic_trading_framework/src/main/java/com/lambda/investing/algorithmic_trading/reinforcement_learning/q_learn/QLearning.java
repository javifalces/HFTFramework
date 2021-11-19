package com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn;

// Catalano Machine Learning Library
// The Catalano Framework
//
// Copyright © Diego Catalano, 2013
// diego.catalano at live.com
//
// Copyright © Andrew Kirillov, 2007-2008
// andrew.kirillov@gmail.com
//
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Lesser General Public
//    License as published by the Free Software Foundation; either
//    version 2.1 of the License, or (at your option) any later version.
//
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public
//    License along with this library; if not, write to the Free Software
//    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
//

import com.lambda.investing.algorithmic_trading.FileUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * The class provides implementation of Q-Learning algorithm, known as
 * off-policy Temporal Difference control.
 * <p>
 * * Learning rate, [0, 1].
 * * The value determines the amount of updates Q-function receives
 * * during learning. The greater the value, the more updates the function receives.
 * * The lower the value, the less updates it receives.
 * <p>
 * Discount factor for the expected summary reward. The value serves as
 * * multiplier for the expected reward. So if the value is set to 1,
 * * then the expected summary reward is not discounted. If the value is getting
 * * smaller, then smaller amount of the expected reward is used for actions'
 * * estimates update.
 *
 * @author Diego Catalano
 */

@Getter @Setter public class QLearning {

	protected static String CSV_SEPARATOR = ",";
	protected Logger logger = LogManager.getLogger(QLearning.class);
	// amount of possible states
	private int states;
	// amount of possible actions
	protected int actions;
	// q-values
	protected double[][] memoryReplay;//states rows, actions columns
	// exploration policy
	private IExplorationPolicy explorationPolicy;

	// discount factor
	protected double discountFactor = 0.95;
	// learning rate
	protected double learningRate = 0.25;

	/**
	 * Initializes a new instance of the QLearning class.
	 *
	 * @param states            Amount of possible states.
	 * @param actions           Amount of possible actions.
	 * @param explorationPolicy Exploration policy.
	 */
	public QLearning(int states, int actions, IExplorationPolicy explorationPolicy, double discountFactor,
			double learningRate) {
		this.states = states;
		this.actions = actions;
		this.explorationPolicy = explorationPolicy;
		this.discountFactor = discountFactor;
		this.learningRate = learningRate;

		// create Q-array
		//		memoryReplay = new double[states][];
		//		for (int i = 0; i < states; i++) {
		//			memoryReplay[i] = new double[actions];
		//		}
		//
		//		// do randomization
		//		if (randomize) {
		//			Random r = new Random();
		//
		//			for (int i = 0; i < states; i++) {
		//				for (int j = 0; j < actions; j++) {
		//					memoryReplay[i][j] = r.nextDouble() / 10;
		//				}
		//			}
		//		}
		logger.info("QLearning instance with {} states and {} actions with discountFactor {} and learningRate {}",
				this.states, this.actions, this.discountFactor, this.learningRate);
	}

	public void init(boolean randomize) {
		// create Q-array
		memoryReplay = new double[states][];
		for (int i = 0; i < states; i++) {
			memoryReplay[i] = new double[actions];
		}

		// do randomization
		if (randomize) {
			Random r = new Random();

			for (int i = 0; i < states; i++) {
				for (int j = 0; j < actions; j++) {
					memoryReplay[i][j] = r.nextDouble() / 10;
				}
			}
		}

	}

	public void enablePeriodPersist(String filename) {
		AutoSaverFile autoSaverFile = new AutoSaverFile(filename);
		new Thread(autoSaverFile, "qLearn_autosaver_" + filename).start();
	}

	/**
	 * Get next action from the specified state.
	 *
	 * @param state Current state to get an action for.
	 * @return Returns the action for the state.
	 */
	public int GetAction(int state) {
		return explorationPolicy.ChooseAction(memoryReplay[state]);
	}

	/**
	 * Update Q-function's value for the previous state-action pair.
	 *
	 * @param previousState Previous state.
	 * @param action        Action, which leads from previous to the next state.
	 * @param reward        Reward value, received by taking specified action from previous state.
	 * @param nextState     Next state.
	 */
	public void updateState(int previousState, int action, double reward, int nextState) {
		// next state's action estimations
		double[] nextActionEstimations = memoryReplay[nextState];
		// find maximum expected summary reward from the next state
		double maxNextExpectedReward = nextActionEstimations[0];

		for (int i = 1; i < actions; i++) {
			if (nextActionEstimations[i] > maxNextExpectedReward)
				maxNextExpectedReward = nextActionEstimations[i];
		}

		// previous state's action estimations
		double[] previousActionEstimations = memoryReplay[previousState];
		// update expected summary reward of the previous state
		double prevQValue = previousActionEstimations[action];
		double newQValue =
				prevQValue * (1.0 - learningRate) + (learningRate * (reward + discountFactor * maxNextExpectedReward));

		memoryReplay[previousState][action] = newQValue;

	}

	public void saveMemory(String filepath) throws IOException {
		FileUtils.persistArray(memoryReplay, filepath, CSV_SEPARATOR);
	}

	public void loadMemory(String filepath) throws IOException {

		BufferedReader csvReader = new BufferedReader(new FileReader(filepath));
		// we don't know the amount of data ahead of time so we use lists

		Map<Integer, List<Double>> colMap = new HashMap<>();
		String row;
		int rowsTotal = 0;

		while ((row = csvReader.readLine()) != null) {
			String[] data = row.split(CSV_SEPARATOR);
			for (int column = 0; column < data.length; column++) {
				List<Double> columnList = colMap.getOrDefault(column, new ArrayList<>());
				columnList.add(Double.parseDouble(data[column]));
				colMap.put(column, columnList);
			}
			rowsTotal++;
		}
		csvReader.close();
		int columnsTotal = colMap.size();

		//transform colMap into array
		double[][] loadedQvalues = new double[rowsTotal][columnsTotal];//states rows , actions columns
		this.states = rowsTotal;
		this.actions = columnsTotal;
		for (int column : colMap.keySet()) {
			List<Double> rows = colMap.get(column);
			int rowIter = 0;
			for (double rowVal : rows) {
				loadedQvalues[rowIter][column] = rowVal;
				rowIter++;
			}
		}

		this.memoryReplay = loadedQvalues;
		System.out.println(
				String.format("loaded a qMatrix of %d rows-states and %d columns-actions", this.memoryReplay.length,
						this.memoryReplay[0].length));

		logger.info(String.format("loaded a qMatrix of %d rows-states and %d columns-actions from %s",
				this.memoryReplay.length, this.memoryReplay[0].length, filepath));

	}

	private class AutoSaverFile implements Runnable {

		private String filename;

		public AutoSaverFile(String filename) {
			this.filename = filename;
		}

		@Override public void run() {
			while (true) {

				try {
					saveMemory(this.filename);
				} catch (IOException e) {
					logger.error("cant save qMatrix periodically ", e);
				}

				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		}
	}

}