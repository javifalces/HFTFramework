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
 * CollectionLabeledSentenceProvider.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.provider;

import java.util.List;

/**
 * Extend CollectionLabeledSentenceProvider from DL4J to support setting numClasses dynamically for
 * regression tasks.
 *
 * @author Steven Lang
 */
public class CollectionLabeledSentenceProvider
		extends org.deeplearning4j.iterator.provider.CollectionLabeledSentenceProvider {

	/**
	 * Number of classes
	 */
	private final int numClasses;

	public CollectionLabeledSentenceProvider(List<String> sentences, List<String> labelsForSentences, int numClasses) {
		super(sentences, labelsForSentences, null);
		this.numClasses = numClasses;
	}

	@Override public int numLabelClasses() {
		return numClasses;
	}
}
