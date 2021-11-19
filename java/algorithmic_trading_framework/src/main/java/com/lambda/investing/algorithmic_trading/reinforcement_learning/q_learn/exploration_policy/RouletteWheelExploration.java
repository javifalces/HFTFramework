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
 * <para>The class implements roulette whell exploration policy. Acording to the policy,
 * action <b>a</b> at state <b>s</b> is selected with the next probability:</para>
 * <code lang="none">
 * Q( s, a )
 * p( s, a ) = ------------------
 * SUM( Q( s, b ) )
 * b
 * </code>
 * <para>where <b>Q(s, a)</b> is action's <b>a</b> estimation (usefulness) at state <b>s</b>.</para>
 *
 * <para><note>The exploration policy may be applied only in cases, when action estimates (usefulness)
 * are represented with positive value greater then 0.</note></para>
 *
 * @author Diego Catalano
 */

@Getter @Setter public class RouletteWheelExploration implements IExplorationPolicy {

	private Random r = new Random();

	/**
	 * Initializes a new instance of the RouletteWheelExploration class.
	 */
	public RouletteWheelExploration() {
	}

	@Override public int ChooseAction(double[] actionEstimates) {
		// actions count
		int actionsCount = actionEstimates.length;
		// actions sum
		double sum = 0, estimateSum = 0;

		for (int i = 0; i < actionsCount; i++) {
			estimateSum += actionEstimates[i];
		}

		// get random number, which determines which action to choose
		double actionRandomNumber = r.nextDouble();

		for (int i = 0; i < actionsCount; i++) {
			sum += actionEstimates[i] / estimateSum;
			if (actionRandomNumber <= sum)
				return i;
		}

		return actionsCount - 1;
	}
}