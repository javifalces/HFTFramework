package com.lambda.investing.algorithmic_trading.reinforcement_learning.reinforce;

import com.google.common.primitives.Doubles;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.MemoryReplayModel;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainNNUtils;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.action.AbstractAction;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.DeepQLearning;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.IExplorationPolicy;
import com.lambda.investing.algorithmic_trading.reinforcement_learning.state.AbstractState;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

import static com.lambda.investing.algorithmic_trading.reinforcement_learning.TrainNNUtils.*;
import static java.lang.Math.log;

public class Reinforce extends DeepQLearning {

	private static boolean EXPLORE_RANDOM_ENABLE = true;

	/**
	 * Initializes a new instance of the Custom Actor critic class.
	 * <p>
	 * Predict will be a classifier with actions 0-1
	 * Target will be a regressor on q values
	 *
	 * @param state             class states.
	 * @param action            class actions.
	 * @param explorationPolicy not used
	 * @param maxMemorySize
	 * @param predictModel
	 * @param targetModel
	 * @param isRNN
	 * @param discountFactor
	 * @param learningRate
	 */
	public Reinforce(AbstractState state, AbstractAction action, IExplorationPolicy explorationPolicy,
			int maxMemorySize, MemoryReplayModel predictModel, MemoryReplayModel targetModel, boolean isRNN,
			double discountFactor, double learningRate, int trainingPredictIterationPeriod,
			int trainingTargetIterationPeriod) throws Exception {
		super(state, action, explorationPolicy, maxMemorySize, predictModel, targetModel, isRNN, discountFactor,
				learningRate, trainingPredictIterationPeriod, trainingTargetIterationPeriod);
	}

	public static <E> E getWeightedRandom(Map<E, Double> weights, Random random) {
		E result = null;
		double bestValue = Double.MAX_VALUE;

		for (E element : weights.keySet()) {
			double value = -log(random.nextDouble()) / weights.get(element);

			if (value < bestValue) {
				bestValue = value;
				result = element;
			}
		}
		return result;
	}

	//	/**
	//	 * Q_(i+1) (s,a)=critic(s,a)*(1-α)+α[R(s,a)+γ_d E(critic(s,a)*actor(s,a) )]
	//	 *
	//	 * @param previousStateArr
	//	 * @param reward
	//	 * @param currentQValue
	//	 * @param predictedQValue
	//	 * @return updated q value
	//	 */
	//	public double calculateQValue(double[] previousStateArr, double reward, double currentQValue,
	//			double predictedQValue) {
	//
	//		if (learningRate == 1 && discountFactor == 0) {
	//			//γ_d=0 and α=1 => Q_(i+1) = R(s,a)
	//			return reward;
	//		}
	//
	//		double[] actorPolicy = getPredictOutput(previousStateArr);//binary 0-1
	//		double[] criticPolicy = getTargetOutput(previousStateArr);//rewards
	//
	//		double outputMultiply = 0.0;
	//		double denominator = 0.0;
	//		for (int iterator = 0; iterator < actorPolicy.length; iterator++) {
	//			outputMultiply += criticPolicy[iterator] * actorPolicy[iterator];
	//			denominator += criticPolicy[iterator];
	//		}
	//		double expectedReward = outputMultiply / denominator;
	//		double currentQValueDiscounted = (1.0 - learningRate) * currentQValue;
	//		double nextQValuePredictionDiscounted = learningRate * (reward + (discountFactor * expectedReward));
	//		return currentQValueDiscounted + nextQValuePredictionDiscounted;
	//	}

	public int GetAction(AbstractState lastState) {

		//explore
		if (EXPLORE_RANDOM_ENABLE && epsilon > EXPLORE_MODE_TRESHOLD) {

			int randomAction = 0;
			double[] rewardSaved = this.getRewardsMemory(lastState.getCurrentStateRounded());

			if (epsilon > EXPLORE_MODE_TRESHOLD && rewardSaved != null) {
				//force explore
				List<Integer> actionsSameAsDefault = new ArrayList<>();
				for (int index = 0; index < rewardSaved.length; index++) {
					double rewardAction = rewardSaved[index];
					boolean isSameScoreAsDefault = isSameDouble(rewardAction, DEFAULT_PREDICTION_ACTION_SCORE, 1E-8);
					if (isSameScoreAsDefault) {
						actionsSameAsDefault.add(index);
					}
				}
				if (actionsSameAsDefault.size() > 0) {
					//force this action
					//randomize on nonBest
					int randomActionExplore = r.nextInt(actionsSameAsDefault.size());
					randomAction = actionsSameAsDefault.get(randomActionExplore);
				}

			} else {
				//normal explore
				int numberActions = getActions();
				if (numberActions > 1) {
					randomAction = r.nextInt(numberActions);
				}
			}
			return randomAction;

		}

		//exploit
		//https://stackoverflow.com/questions/6737283/weighted-randomness-in-java
		double[] currentState = lastState.getCurrentStateRounded();
		double[] actionScoreEstimation = getProbabilities(currentState);
		/// probability weighted action chosen!
		if (actionScoreEstimation == null) {
			int numberActions = getActions();
			int randomAction = 0;
			if (numberActions > 1) {
				randomAction = r.nextInt(numberActions);
			}
			counterExplore++;
			return randomAction;
		}
		TreeMap<Integer, Double> probabilitiesAction = new TreeMap<>();

		for (int index = 0; index < actionScoreEstimation.length; index++) {
			probabilitiesAction.put(index, actionScoreEstimation[index]);
		}
		int actionOut = getWeightedRandom(probabilitiesAction, r);
		counterExploit++;
		return actionOut;
	}

	/**
	 * small fine tuning to get all q values from the target nn , the regressor
	 *
	 * @param state
	 * @param action
	 * @return
	 */
	protected Double getQValue(double[] state, int action) {
		if (asQLearn) {
			//get direclty from memory replay
			double[] actions = getRewards(state);
			if (actions != null) {
				return actions[action];
			}
			return DEFAULT_PREDICTION_ACTION_SCORE;
		}
		double[] targetQValue = getPredictOutput(state);
		if (targetQValue != null) {
			return targetQValue[action];
		}
		return null;
	}

	public double[] getPredictOutput(double[] state) {
		if (asQLearn) {
			//get direclty from memory replay
			return getRewards(state);
		}
		double[] output = this.predictModel.predict(state);
		//normalize it
		if (output == null) {
			return null;
		}
		double sum = 0;
		for (int index = 0; index < output.length; index++) {
			sum += output[index];
		}
		for (int index = 0; index < output.length; index++) {
			output[index] = output[index] / sum;
		}
		return output;

	}

	public double[][] getTargetTrainQVal() {
		double[][] validArr = getArrayValid();

		logger.info("training target array of {} rows and {} columns", validArr.length, action.getNumberActions());
		double[][] rewardArr = getColumnsArray(validArr, getStateColumns(),
				getStateColumns() + action.getNumberActions());
		double[][] states = getColumnsArray(validArr, 0, getStateColumns());
		double[][] nextStates = getColumnsArray(validArr, getStateColumns() + action.getNumberActions(),
				validArr[0].length);
		//i want to get q values , not interested in prob
		return getTargetTrainValues(states, nextStates, rewardArr, false);

	}

	public double[][] getTargetTrain() {
		return super.getTargetTrain();
	}

	public double[][] getInputTrain() {
		return super.getInputTrain();
	}

	protected double[][] getTargetTrainValues(double[][] states, double[][] nextStates, double[][] rewardArr) {
		//we call this method to train probabilities -> interested in highest q val without prob
		return getTargetTrainValues(states, nextStates, rewardArr, false);
	}

	/**
	 * We get the value of the rewards
	 *
	 * @param states
	 * @param nextStates
	 * @param rewardArr
	 * @param multiplyProb for training target -> multiply rewards by prob
	 * @return
	 */
	protected double[][] getTargetTrainValues(double[][] states, double[][] nextStates, double[][] rewardArr,
			boolean multiplyProb) {
		assert states[0].length == getStateColumns();
		assert nextStates[0].length == getStateColumns();
		assert rewardArr[0].length == getActions();
		MemoryReplayModel actorModel = predictModel;
		MemoryReplayModel criticModel = targetModel;

		double[][] target = new double[rewardArr.length][rewardArr[0].length];
		if (multiplyProb && actorModel == null || !actorModel.isTrained()) {
			logger.warn("predictModel(actor) is not trained and required to getTargetTrainValues");
		}
		if (criticModel == null || !criticModel.isTrained()) {
			logger.warn("targetModel(critic) is not trained and required to getTargetTrainValues");
		}
		for (int row = 0; row < rewardArr.length; row++) {
			double[] nextState = nextStates[row];
			double[] reward = rewardArr[row];
			double[] state = states[row];

			double[] probAction = getProbabilities(state);
			if (multiplyProb && probAction == null) {
				probAction = new double[reward.length];
				Arrays.fill(probAction, 1.0);
			}

			double[] qTarget = criticModel.predict(state);
			if (qTarget == null) {
				qTarget = reward;
			}

			//override on the reward only because is where the error is
			List<Integer> actionChosen = new ArrayList<>();
			for (int column = 0; column < reward.length; column++) {
				if (Double.isFinite(reward[column]) && reward[column] != DEFAULT_PREDICTION_ACTION_SCORE) {
					qTarget[column] = reward[column];
					actionChosen.add(column);
				}
			}
			if (actionChosen.size() == 0) {
				logger.warn("actionChosen can't be found to train on new QValue -> not correction row");
			}

			double discount = discountFactor;

			//next state q value
			double[] nextRewards = actorModel.predict(nextState);
			double[] nextProb = getProbabilities(nextState);
			double nextRewardsValue = 0.0;
			if (multiplyProb) {
				if (nextRewards != null && nextProb != null && nextRewards.length == nextProb.length) {
					for (int index = 0; index < nextRewards.length; index++) {
						nextRewardsValue += nextRewards[index] * nextProb[index];
					}

				}
			} else {
				if (nextRewards != null) {
					nextRewardsValue = Doubles.max(nextRewards);
				}
			}

			for (int column = 0; column < reward.length; column++) {
				if (actionChosen.contains(column)) {
					target[row][column] = reward[column] + discount * nextRewardsValue;//*probAction[column]?
					if (multiplyProb) {
						target[row][column] =
								reward[column] * probAction[column] + discount * nextRewardsValue;//*probAction[column]?
					}
				} else {
					target[row][column] = qTarget[column];
					if (multiplyProb) {
						target[row][column] *= probAction[column];
					}
				}

			}

		}
		return target;
	}

	/**
	 * Training target for predict network => best action to do stochastic output
	 *
	 * @return
	 */
	public double[][] getTargetTrainClassif() {

		double[][] validArr = getArrayValidClassif();
		logger.info("training target array of {} rows and {} columns", validArr.length, action.getNumberActions());
		double[][] rewardArr = getColumnsArray(validArr, getStateColumns(),
				getStateColumns() + action.getNumberActions());
		double[][] states = getColumnsArray(validArr, 0, getStateColumns());
		double[][] nextStates = getColumnsArray(validArr, getStateColumns() + action.getNumberActions(),
				validArr[0].length);

		return getTargetClassification(getTargetTrainValues(states, nextStates, rewardArr));
	}

	public double[][] getInputTrainClassif() {
		double[][] validArr = getArrayValidClassif();
		logger.info("training input array of {} rows and {} columns", validArr.length, getStateColumns());
		return getInputTrainValues(validArr);

	}

	public double[] getRewards(double[] stateToSearch) {
		return targetModel.predict(stateToSearch);
	}

	public double[] getRewardsMemory(double[] stateToSearch) {
		return super.getRewards(stateToSearch);
	}

	public double[] getProbabilities(double[] stateToSearch) {
		return getPredictOutput(stateToSearch);
	}

	/**
	 * Get array of states and action rewards that reward has some values !=0
	 *
	 * @return
	 */
	protected double[][] getArrayValidClassif() {
		double[][] validArr = ArrayUtils
				.subarray(memoryReplay, 0, getNextIndexMemoryReplay());//we are going to clean it
		return TrainNNUtils.getArrayValid(validArr, getStateColumns(), action.getNumberActions(), true);
	}

	/**
	 * https://towardsdatascience.com/understanding-actor-critic-methods-931b97b6df3f
	 * https://python.plainenglish.io/introduction-to-reinforcement-learning-policy-gradient-c30fa833f7a9
	 */

	/////trainer
	protected void updateGradientsTarget() {
		trainCritic();
	}

	protected void updateGradientsPredict() {
		//reinforce logic
		//classification  output!
		trainPrediction();
	}

	protected boolean trainPrediction() {
		if (asQLearn) {
			return true;
		}
		return trainActor();
	}

	protected boolean trainActor() {
		//classification
		double[][] input = getInputTrain();//states
		double[][] target = getTargetClassification(getTargetTrain());
		assert input.length == target.length;
		boolean output = this.predictModel.train(input, target);
		setPredictModel(this.predictModel);
		return output;
	}

	protected boolean trainTarget() {
		//critic
		if (asQLearn) {
			return true;
		}
		return trainCritic();
	}

	protected boolean trainCritic() {
		double[][] input = getInputTrain();
		double[][] target = getTargetTrainQVal();

		assert input.length == target.length;
		logger.info("training target ");
		boolean output = this.targetModel.train(input, target);
		setTargetModel(this.targetModel);
		return output;
	}

}
