/*
 * WekaDeeplearning4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WekaDeeplearning4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WekaDeeplearning4j.  If not, see <https://www.gnu.org/licenses/>.
 *
 * NoOp.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.updater;

/**
 * A WEKA version of DeepLearning4j's ConstantScheduleImpl.
 *
 * @author Steven Lang
 */
public class NoOp extends Updater<org.nd4j.linalg.learning.config.NoOp> {

	private static final long serialVersionUID = 3503205924392465662L;

	@Override public void initializeBackend() {
		backend = new org.nd4j.linalg.learning.config.NoOp();
	}
}
