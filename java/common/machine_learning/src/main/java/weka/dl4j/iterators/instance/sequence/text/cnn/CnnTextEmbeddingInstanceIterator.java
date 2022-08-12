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
 * CnnTextEmbeddingInstanceIterator.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.instance.sequence.text.cnn;

import java.util.Enumeration;

import org.deeplearning4j.iterator.LabeledSentenceProvider;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import weka.core.Instances;
import weka.core.InvalidInputDataException;
import weka.core.Option;
import weka.dl4j.iterators.dataset.sequence.text.cnn.CnnSentenceDataSetIterator;
import weka.dl4j.iterators.instance.sequence.text.AbstractTextEmbeddingIterator;

/**
 * Iterator that constructs datasets from text data for convolutional networks.
 *
 * @author Steven Lang
 */
public class CnnTextEmbeddingInstanceIterator extends AbstractTextEmbeddingIterator {

	private static final long serialVersionUID = 3417451906101970927L;

	@Override public DataSetIterator getDataSetIterator(Instances data, int seed, int batchSize) {
		initialize();
		LabeledSentenceProvider clsp = getSentenceProvider(data);
		return new CnnSentenceDataSetIterator.Builder().stopwords(stopwords).wordVectors(wordVectors)
				.tokenizerFactory(tokenizerFactory.getBackend()).sentenceProvider(clsp).minibatchSize(batchSize)
				.maxSentenceLength(truncateLength).useNormalizedWordVectors(false).sentencesAlongHeight(true).build();
	}

	@Override public void validate(Instances data) throws InvalidInputDataException {
		if (!getWordVectorLocation().isFile()) {
			throw new InvalidInputDataException("File not valid: " + getWordVectorLocation());
		}
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override public Enumeration<Option> listOptions() {
		return Option.listOptionsForClassHierarchy(this.getClass(), super.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override public String[] getOptions() {
		return Option.getOptionsForHierarchy(this, super.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		Option.setOptionsForHierarchy(options, this, super.getClass());
	}

}
