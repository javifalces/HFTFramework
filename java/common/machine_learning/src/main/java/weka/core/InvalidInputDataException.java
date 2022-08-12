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
 * InvalidInputDataException.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

/**
 * Exception raised in the case of an invalid input data
 *
 * @author Steven Lang
 */
public class InvalidInputDataException extends WekaException {

	private static final long serialVersionUID = -388372062851727428L;

	public InvalidInputDataException(String message) {
		super(message);
	}

	public InvalidInputDataException(String message, Throwable cause) {
		super(message, cause);
	}
}
