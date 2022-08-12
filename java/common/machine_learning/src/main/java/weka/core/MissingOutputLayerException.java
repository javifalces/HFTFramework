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
 * MissingOutputLayerException.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

/**
 * Exception raised in the case of a missing output layer as last layer
 *
 * @author Steven Lang
 */
public class MissingOutputLayerException extends WekaException {

	private static final long serialVersionUID = 1038306995981039092L;

	public MissingOutputLayerException(String message) {
		super(message);
	}

	public MissingOutputLayerException(String message, Throwable cause) {
		super(message, cause);
	}
}
