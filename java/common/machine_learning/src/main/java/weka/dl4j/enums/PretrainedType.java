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
 * PoolingType.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.enums;

import weka.dl4j.ApiWrapper;

/**
 * Proxy Enum for {@link org.deeplearning4j.zoo.PretrainedType}. This is necessary as Weka's
 * run script cannot find the enum classes during the option parsing as they reside in the Dl4j
 * backend and are at that time not visible to the class loader.
 *
 * @author Rhys Compton
 */
public enum PretrainedType implements ApiWrapper<org.deeplearning4j.zoo.PretrainedType> {
	/**
	 * Pretrained on ImageNet
	 */
	IMAGENET,
	/**
	 * Pretrained on MNIST.
	 */
	MNIST,
	/**
	 * Pretrained on VGGFace.
	 */
	VGGFACE,
	/**
	 * Not pretrained.
	 */
	NONE;

	/**
	 * Parse backend pooling type and return weka enum implementation.
	 *
	 * @param pretrainedType Pooling type
	 * @return Weka pretrained type enum implementation
	 */
	public static PretrainedType fromBackend(org.deeplearning4j.zoo.PretrainedType pretrainedType) {
		return valueOf(pretrainedType.name());
	}

	@Override public org.deeplearning4j.zoo.PretrainedType getBackend() {
		return org.deeplearning4j.zoo.PretrainedType.valueOf(this.name());
	}

	@Override public void setBackend(org.deeplearning4j.zoo.PretrainedType pretrainedType) {
		// Do nothing as this enum does not have a state
	}

	@Override public void initializeBackend() {
		// Do nothing as this enum does not have a state
	}
}
