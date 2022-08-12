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
 * Disabled.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.dropout;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.deeplearning4j.nn.conf.dropout.Dropout;
import weka.dl4j.dropout.Disabled.DisabledDropoutImpl;

/**
 * Disabled dropout.
 *
 * @author Steven Lang
 */
@EqualsAndHashCode(callSuper = true) @ToString public class Disabled extends AbstractDropout<DisabledDropoutImpl> {

	private static final long serialVersionUID = 8082864981844682636L;

	@Override public void initializeBackend() {
		backend = null;
	}

	/**
	 * Dummy dropout implementation.
	 */
	protected class DisabledDropoutImpl extends Dropout {

		private static final long serialVersionUID = 5933930276882455322L;

		public DisabledDropoutImpl(double activationRetainProbability) {
			super(activationRetainProbability);
		}
	}
}
