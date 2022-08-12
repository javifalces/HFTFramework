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
 * Dl4jWordsFromFile.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.stopwords;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fracpete, Felipe Bravo-Marquez
 */
public class Dl4jWordsFromFile extends Dl4jAbstractFileBasedStopwords {

	/**
	 * for serialization.
	 */
	private static final long serialVersionUID = -722795295494945193L;

	/**
	 * The list of stopwords.
	 */
	protected List<String> stopWords;

	/**
	 * Returns a string describing the stopwords scheme.
	 *
	 * @return a description suitable for displaying in the gui
	 */
	@Override public String globalInfo() {
		return "Uses the stopwords located in the specified file (ignored _if "
				+ "pointing to a directory). One stopword per line. Lines "
				+ "starting with '#' are considered comments and ignored.";
	}

	/**
	 * Returns the tip text for this property.
	 *
	 * @return tip text for this property suitable for displaying in the explorer/experimenter gui
	 */
	@Override public String stopwordsTipText() {
		return "The file containing the stopwords.";
	}

	/**
	 * Performs intialization of the scheme.
	 */
	@Override public void initialize() {
		List<String> words;

		stopWords = new ArrayList<String>();
		words = read();
		for (String word : words) {
			// comment?
			if (!word.startsWith("#")) {
				stopWords.add(word);
			}
		}
	}

	/**
	 * Returns true if the given string is a stop word.
	 *
	 * @param word the word to test
	 * @return true if the word is a stopword
	 */
	@Override protected synchronized boolean is(String word) {
		return stopWords.contains(word.trim().toLowerCase());
	}

	/* (non-Javadoc)
	 * @see weka.dl4j.text.stopwords.Dl4jAbstractStopwords#getStopList()
	 */
	@Override public List<String> getStopList() {
		return stopWords;
	}
}
