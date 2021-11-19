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

/**
 * The interface describes exploration policies, which are used in Reinforcement
 * Learning to explore state space.
 *
 * @author Diego Catalano
 */
public interface IExplorationPolicy {

	/**
	 * The method chooses an action depending on the provided estimates. the
	 * estimates can be any sort of estimate, which values usefulness of the action
	 * (expected summary reward, discounted reward, etc).
	 *
	 * @param actionEstimates Action estimates.
	 * @return Returns selected action.
	 */
	public int ChooseAction(double[] actionEstimates);
}