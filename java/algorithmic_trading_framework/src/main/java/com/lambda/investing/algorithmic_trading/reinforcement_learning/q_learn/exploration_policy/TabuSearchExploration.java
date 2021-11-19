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

import java.util.Arrays;

/**
 * Tabu search exploration policy.
 * The class implements simple tabu search exploration policy,
 * allowing to set certain actions as tabu for a specified amount of
 * iterations. The actual exploration and choosing from non-tabu actions
 * is done by base exploration policy.
 * <p>
 * Base exploration policy is the policy, which is used to choose from non-tabu actions.
 *
 * @author Diego Catalano
 */

@Getter @Setter public class TabuSearchExploration implements IExplorationPolicy {

	private int actions;
	private int[] tabuActions = null;
	private IExplorationPolicy basePolicy;

	/**
	 * Initializes a new instance of the TabuSearchExploration
	 *
	 * @param actions    Total actions count.
	 * @param basePolicy Base exploration policy.
	 */
	public TabuSearchExploration(int actions, IExplorationPolicy basePolicy) {
		this.actions = actions;
		this.basePolicy = basePolicy;
		tabuActions = new int[actions];
	}

	@Override public int ChooseAction(double[] actionEstimates) {
		// get amount of non-tabu actions
		int nonTabuActions = actions;
		for (int i = 0; i < actions; i++) {
			if (tabuActions[i] != 0) {
				nonTabuActions--;
			}
		}

		// allowed actions
		double[] allowedActionEstimates = new double[nonTabuActions];
		int[] allowedActionMap = new int[nonTabuActions];

		for (int i = 0, j = 0; i < actions; i++) {
			if (tabuActions[i] == 0) {
				// allowed action
				allowedActionEstimates[j] = actionEstimates[i];
				allowedActionMap[j] = i;
				j++;
			} else {
				// decrease tabu time of tabu action
				tabuActions[i]--;
			}
		}

		return allowedActionMap[basePolicy.ChooseAction(allowedActionEstimates)];
	}

	/**
	 * Reset tabu list.
	 * Clears tabu list making all actions allowed.
	 */
	public void ResetTabuList() {
		//Array.Clear( tabuActions, 0, actions );
		Arrays.fill(tabuActions, 0, actions, 0);
	}

}