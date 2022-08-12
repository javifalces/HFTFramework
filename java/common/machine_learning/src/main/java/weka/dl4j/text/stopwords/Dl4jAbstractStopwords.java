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
 * Dl4jAbstractStopwords.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.stopwords;

import java.util.List;

import weka.core.stopwords.AbstractStopwords;

/**
 * Abstract stopwords handler for DL4j.
 *
 * @author Felipe Bravo-Marquez
 */
public abstract class Dl4jAbstractStopwords extends AbstractStopwords {

	/**
	 * for serialization
	 */
	private static final long serialVersionUID = -2167994358835350653L;

	/**
	 * Returns the list of stopwords.
	 *
	 * @return the list of stopwords
	 */
	public abstract List<String> getStopList();

	/**
	 * initializes the dictionary
	 */
	public abstract void initialize();
}
