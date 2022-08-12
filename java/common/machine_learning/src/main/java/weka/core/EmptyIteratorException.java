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
 * EmptyIteratorException.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

/**
 * Exception raised when the iterator was unexpectedly empty
 *
 * @author Steven Lang
 */
public class EmptyIteratorException extends WekaException {

	private static final long serialVersionUID = 7159773687653762115L;

	public EmptyIteratorException(String message) {
		super(message);
	}

	public EmptyIteratorException(String message, Throwable cause) {
		super(message, cause);
	}
}
