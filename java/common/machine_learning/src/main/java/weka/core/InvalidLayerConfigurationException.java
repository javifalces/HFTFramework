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
 * InvalidLayerConfigurationException.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import weka.dl4j.layers.Layer;

/**
 * Exception raised when the the configuration is invalid.
 *
 * @author Steven Lang
 */
public class InvalidLayerConfigurationException extends WekaException {

	private static final long serialVersionUID = -8089134131947363159L;

	public InvalidLayerConfigurationException(String message, Layer layer) {
		super("Layer=" + layer.getLayerName() + ": " + message);
	}

	public InvalidLayerConfigurationException(String message, Layer layer, Throwable cause) {
		super("Layer=" + layer.getLayerName() + ": " + message, cause);
	}
}
