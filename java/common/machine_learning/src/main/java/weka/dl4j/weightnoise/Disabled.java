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

package weka.dl4j.weightnoise;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import weka.core.OptionHandler;

/**
 * Disabled option for WeightNoise.
 *
 * @author Steven Lang
 */
@EqualsAndHashCode(callSuper = true) @ToString public class Disabled extends AbstractWeightNoise
		implements OptionHandler, Serializable {

	private static final long serialVersionUID = 4568626187488846101L;

	@Override public void initializeBackend() {
		backend = null;
	}

}
