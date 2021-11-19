package com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.exploration_policy;

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

import com.lambda.investing.algorithmic_trading.reinforcement_learning.q_learn.IExplorationPolicy;
import lombok.Getter;
import lombok.Setter;

import java.util.Random;

/**
 * <para>The class implements exploration policy base on Boltzmann distribution.
 * Acording to the policy, action <b>a</b> at state <b>s</b> is selected with the next probability:</para>
 * <code lang="none">
 * exp( Q( s, a ) / t )
 * p( s, a ) = -----------------------------
 * SUM( exp( Q( s, b ) / t ) )
 * b
 * </code>
 * <para>where <b>Q(s, a)</b> is action's <b>a</b> estimation (usefulness) at state <b>s</b> and
 * <b>t</b> is Temperature.
 * <p>
 * The property sets the balance between exploration and greedy actions.
 * If temperature is low, then the policy tends to be more greedy.
 *
 * @author Diego Catalano
 */

@Getter @Setter public class BoltzmannExploration implements IExplorationPolicy {

	double temperature;
	private Random r = new Random();

	/**
	 * Initializes a new instance of the BoltzmannExploration class.
	 *
	 * @param temperature Temperature parameter of Boltzmann distribution.
	 */
	public BoltzmannExploration(double temperature) {
		this.temperature = temperature;
	}

	/**
	 * The method chooses an action depending on the provided estimates.
	 * The estimates can be any sort of estimate, which values usefulness of the action (expected summary reward, discounted reward, etc).
	 *
	 * @param actionEstimates Action Estimates.
	 * @return Return selected action.
	 */
	@Override public int ChooseAction(double[] actionEstimates) {
		// actions count
		int actionsCount = actionEstimates.length;
		// action probabilities
		double[] actionProbabilities = new double[actionsCount];
		// actions sum
		double sum = 0, probabilitiesSum = 0;

		for (int i = 0; i < actionsCount; i++) {
			double actionProbability = Math.exp(actionEstimates[i] / temperature);

			actionProbabilities[i] = actionProbability;
			probabilitiesSum += actionProbability;
		}

		if ((Double.isInfinite(probabilitiesSum)) || (probabilitiesSum == 0)) {
			// do greedy selection in the case of infinity or zero
			double maxReward = actionEstimates[0];
			int greedyAction = 0;

			for (int i = 1; i < actionsCount; i++) {
				if (actionEstimates[i] > maxReward) {
					maxReward = actionEstimates[i];
					greedyAction = i;
				}
			}
			return greedyAction;
		}

		// get random number, which determines which action to choose
		double actionRandomNumber = r.nextDouble();

		for (int i = 0; i < actionsCount; i++) {
			sum += actionProbabilities[i] / probabilitiesSum;
			if (actionRandomNumber <= sum)
				return i;
		}

		return actionsCount - 1;
	}
}