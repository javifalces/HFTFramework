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
 * AlgoMode.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.enums;

import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import weka.dl4j.ApiWrapper;

/**
 * Proxy Enum for {@link org.deeplearning4j.nn.conf.ConvolutionMode}. This is necessary as Weka's
 * run script cannot find the enum classes during the option parsing as they reside in the Dl4j
 * backend and are at that time not visible to the class loader.
 *
 * @author Steven Lang
 */
public enum AlgoMode implements ApiWrapper<ConvolutionLayer.AlgoMode> {
	NO_WORKSPACE, PREFER_FASTEST, USER_SPECIFIED;

	/**
	 * Parse backend algo mode and return weka enum implementation.
	 *
	 * @param algoMode Convolution mode
	 * @return Weka convolution mode enum implementation
	 */
	public static AlgoMode fromBackend(ConvolutionLayer.AlgoMode algoMode) {
		return valueOf(algoMode.name());
	}

	@Override public ConvolutionLayer.AlgoMode getBackend() {
		return ConvolutionLayer.AlgoMode.valueOf(this.name());
	}

	@Override public void setBackend(ConvolutionLayer.AlgoMode newBackend) {
		// Do nothing as this enum does not have a state

	}

	@Override public void initializeBackend() {
		// Do nothing as this enum does not have a state
	}
}
