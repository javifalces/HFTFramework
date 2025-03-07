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
 * AbstractInstanceIterator.java
 * Copyright (C) 2016-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.instance;

import java.io.Serializable;
import java.util.Enumeration;

import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import weka.core.Instances;
import weka.core.InvalidInputDataException;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;

/**
 * An abstract iterator that wraps DataSetIterators around Weka {@link Instances}.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 * @author Steven Lang
 */
public abstract class AbstractInstanceIterator implements OptionHandler, Serializable {

	/**
	 * The ID used for serialization
	 */
	private static final long serialVersionUID = 7440584973810993954L;

	/**
	 * The batch size for the mini batches
	 */
	protected int batchSize = 1;

	/**
	 * Returns the actual iterator.
	 *
	 * @param data the dataset to use
	 * @param seed the seed for the random number generator
	 * @return the iterator
	 * @throws Exception if the constructor cannot be constructed successfully
	 */
	public DataSetIterator getDataSetIterator(Instances data, int seed) throws Exception {

		return getDataSetIterator(data, seed, getTrainBatchSize());
	}

	/**
	 * Returns the actual iterator.
	 *
	 * @param data      the dataset to use
	 * @param seed      the seed for the random number generator
	 * @param batchSize the batch size to use
	 * @return the iterator
	 * @throws Exception if the constructor cannot be constructed successfully
	 */
	public abstract DataSetIterator getDataSetIterator(Instances data, int seed, int batchSize) throws Exception;

	/**
	 * Getting the training batch size
	 *
	 * @return the batch size
	 */
	@OptionMetadata(displayName = "size of mini batch", description = "The mini batch size to use in the iterator (default = 1).", commandLineParamName = "bs", commandLineParamSynopsis = "-bs <int>", displayOrder = 1) public int getTrainBatchSize() {
		return batchSize;
	}

	/**
	 * Setting the training batch size
	 *
	 * @param trainBatchSize the batch size
	 */
	public void setTrainBatchSize(int trainBatchSize) {
		batchSize = trainBatchSize;
	}

	/**
	 * Initialize the iterator
	 */
	public void initialize() {
		// Do nothing by default
	}

	/**
	 * Validates the input dataset
	 *
	 * @param data the input dataset
	 * @throws InvalidInputDataException if validation is unsuccessful
	 */
	public abstract void validate(Instances data) throws InvalidInputDataException;

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override public Enumeration<Option> listOptions() {

		return Option.listOptionsForClass(this.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override public String[] getOptions() {

		return Option.getOptions(this, this.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {

		Option.setOptions(options, this, this.getClass());
	}
}
