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
 * Dl4jNull.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.stopwords;

import java.util.ArrayList;
import java.util.List;

/**
 * <!-- globalinfo-start --> Dummy stopwords scheme, returns an empty list of stopWords.. <!--
 * globalinfo-end -->
 *
 * @author fracpete, Felipe Bravo-Marquez
 */
public class Dl4jNull extends Dl4jAbstractStopwords {

	/**
	 * For serialization.
	 */
	private static final long serialVersionUID = -9129283649432847013L;

	/**
	 * The list of stopwords.
	 */
	protected List<String> stopWords;

	/* (non-Javadoc)
	 * @see weka.dl4j.text.stopwords.Dl4jAbstractStopwords#getStopList()
	 */
	@Override public List<String> getStopList() {

		return stopWords;
	}

	/* (non-Javadoc)
	 * @see weka.dl4j.text.stopwords.Dl4jAbstractStopwords#initialize()
	 */
	@Override public void initialize() {
		stopWords = new ArrayList<String>();
	}

	/**
	 * Returns a string describing the stopwords scheme.
	 *
	 * @return a description suitable for displaying in the gui
	 */
	@Override public String globalInfo() {
		return "Dummy stopwords scheme, returns an empty list of stopWords.";
	}

	/**
	 * Returns true if the given string is a stop word.
	 *
	 * @param word the word to test
	 * @return always false
	 */
	@Override protected boolean is(String word) {
		return false;
	}
}
