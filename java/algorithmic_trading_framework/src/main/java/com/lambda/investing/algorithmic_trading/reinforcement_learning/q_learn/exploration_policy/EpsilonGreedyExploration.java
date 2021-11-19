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
 * <para>The class implements epsilon greedy exploration policy. Acording to the policy,
 * the best action is chosen with probability <b>1-epsilon</b>. Otherwise,
 * with probability <b>epsilon</b>, any other action, except the best one, is
 * chosen randomly.</para>
 * <p>
 * * The value determines the amount of exploration driven by the policy.
 * * If the value is high, then the policy drives more to exploration - choosing random
 * * action, which excludes the best one. If the value is low, then the policy is more
 * * greedy - choosing the beat so far action.
 *
 *
 * <para> According to the policy, the epsilon value is known also as exploration rate. </para>
 *
 * @author Diego Catalano
 */
@Getter @Setter public class EpsilonGreedyExploration implements IExplorationPolicy {

	private double epsilon;

	private Random r = new Random();

	/**
	 * Initializes a new instance of the EpsilonGreedyExploration class.
	 *
	 * @param epsilon Epsilon value (exploration rate).
	 */
	public EpsilonGreedyExploration(double epsilon) {
		this.epsilon = epsilon;
	}

	/**
	 * The method chooses an action depending on the provided estimates. The
	 * estimates can be any sort of estimate, which values usefulness of the action
	 * (expected summary reward, discounted reward, etc).
	 *
	 * @param actionEstimates Action Estimates.
	 * @return Return Selected actions.
	 */
	@Override public int ChooseAction(double[] actionEstimates) {
		int actionsCount = actionEstimates.length;

		// find the best action (greedy)
		double maxReward = actionEstimates[0];
		int greedyAction = 0;

		for (int i = 1; i < actionsCount; i++) {
			if (actionEstimates[i] > maxReward) {
				maxReward = actionEstimates[i];
				greedyAction = i;
			}
		}

		// try to do exploration
		if (r.nextDouble() < epsilon) {
			int randomAction = r.nextInt(actionsCount - 1);

			if (randomAction >= greedyAction)
				randomAction++;

			return randomAction;
		}

		return greedyAction;
	}

}