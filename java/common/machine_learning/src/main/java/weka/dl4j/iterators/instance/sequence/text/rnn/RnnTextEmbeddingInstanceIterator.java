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
 * RnnTextEmbeddingInstanceIterator.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.instance.sequence.text.rnn;

import java.io.IOException;
import java.util.Enumeration;

import lombok.extern.log4j.Log4j2;
import org.deeplearning4j.iterator.LabeledSentenceProvider;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import weka.core.Instances;
import weka.core.InvalidInputDataException;
import weka.core.Option;
import weka.dl4j.iterators.dataset.sequence.text.rnn.RnnTextEmbeddingDataSetIterator;
import weka.dl4j.iterators.instance.sequence.text.AbstractTextEmbeddingIterator;

/**
 * Converts the given Instances object into a DataSet and then constructs and returns a
 * RnnTextEmbeddingInstanceIterator.
 *
 * <p>Assumes the instance with the following attributes:
 *
 * <ul>
 * <li>Text (e.g. a elementwise document)
 * <li>Class
 * </ul>
 *
 * @author Steven Lang
 */
@Log4j2 public class RnnTextEmbeddingInstanceIterator extends AbstractTextEmbeddingIterator {

	/**
	 * The ID used to serialize this class
	 */
	private static final long serialVersionUID = 1316260988724548474L;

	@Override public void validate(Instances data) throws InvalidInputDataException {
		if (!((data.attribute(0).isString() && data.classIndex() == 1) || (data.attribute(1).isString()
				&& data.classIndex() == 0))) {
			throw new InvalidInputDataException("An ARFF is required with a string attribute and a class attribute");
		}
		if (data.numAttributes() != 2) {
			throw new InvalidInputDataException(
					"There must be exactly two attributes: 1) Text 2) Label. " + "The given data consists of " + data
							.numAttributes() + " attributes.");
		}
	}

	/**
	 * Returns the actual iterator.
	 *
	 * @param data      the dataset to use
	 * @param seed      the seed for the random number generator
	 * @param batchSize the batch size to use
	 * @return the DataSetIterator
	 */
	@Override public DataSetIterator getDataSetIterator(Instances data, int seed, int batchSize)
			throws InvalidInputDataException, IOException {
		validate(data);
		initWordVectors();
		final LabeledSentenceProvider prov = getSentenceProvider(data);
		return new RnnTextEmbeddingDataSetIterator(data, wordVectors, tokenizerFactory, tokenPreProcess, stopwords,
				prov, batchSize, truncateLength);
	}

	@Override public void initialize() {
		super.initialize();
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

	public String globalInfo() {
		return "Text iterator that reads documents line wise from an ARFF file. Each document is then "
				+ "processed by the tokenization, stopwords, token-preprocessing and afterwards mapped into "
				+ "an embedding space with the given word-vector model.";
	}
}
