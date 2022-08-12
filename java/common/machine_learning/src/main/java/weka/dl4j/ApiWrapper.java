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
 * ApiWrapper.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j;

/**
 * A general interface to access a backend object of a certain deeplearning4j class which is wrapped
 * in a class to make it usable in weka.
 *
 * @param <T> Class which is to be wrapped.
 * @author Steven Lang
 */
public interface ApiWrapper<T> {

	/**
	 * Access the DL4J backend.
	 *
	 * @return DL4J backend for this wrapper
	 */
	T getBackend();

	/**
	 * Set the DL4J backend.
	 *
	 * @param newBackend Backend that should be wrapped by this class
	 */
	void setBackend(T newBackend);

	/**
	 * Initialize the DL4J backend.
	 */
	void initializeBackend();
}
