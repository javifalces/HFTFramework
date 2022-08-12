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
 * DefaultDataSetIterator.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.dataset;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

/**
 * An nd4j mini-batch iterator that iterates a given dataset.
 *
 * @author Steven Lang
 */
public class DefaultDataSetIterator implements DataSetIterator, Serializable {

	/**
	 * The ID used to serialize this class
	 */
	private static final long serialVersionUID = 5571114918884888578L;

	/**
	 * The dataset to operate on
	 */
	protected DataSet data = null;

	/**
	 * The batch size
	 */
	protected int batchSize = 1;

	/**
	 * The cursor
	 */
	protected int cursor = 0;

	/**
	 * An optional dataset preprocessor
	 */
	protected DataSetPreProcessor preProcessor;

	/**
	 * Constructs a new dataset iterator.
	 *
	 * @param data      The dataset to operate on
	 * @param batchSize The batch size
	 */
	public DefaultDataSetIterator(DataSet data, int batchSize) {

		this.data = data;
		this.batchSize = Math.min(batchSize, data.numExamples());
	}

	/**
	 * Whether another batch of data is still available.
	 *
	 * @return true if another batch is still available
	 */
	@Override public boolean hasNext() {
		return (cursor + batchSize <= data.numExamples());
	}

	/**
	 * Returns the next mini batch of data.
	 *
	 * @return the dataset corresponding to the mini batch
	 */
	@Override public DataSet next() {
		// Apply preprocessor
		if (preProcessor != null) {
			preProcessor.preProcess(data);
		}

		// Special case: getRange() does not work as expected if there is just a single example
		if ((cursor == 0) && (batchSize == 1) && (data.numExamples() == 1)) {
			cursor += batchSize;
			return data;
		}
		int size = Math.min(batchSize, data.numExamples() - cursor);
		DataSet thisBatch = (DataSet) data.getRange(cursor, cursor + size);
		cursor += batchSize;
		return thisBatch;
	}

	/**
	 * Returns a batch of the given size
	 *
	 * @param num the size of the batch to return
	 * @return a mini-batch of the given size
	 */
	@Override public DataSet next(int num) {

		// Apply preprocessor
		if (preProcessor != null) {
			preProcessor.preProcess(data);
		}

		// Special case: getRange() does not work as expected if there is just a single example
		if ((cursor == 0) && (num == 1) && (data.numExamples() == 1)) {
			return data;
		}

		DataSet thisBatch = (DataSet) data.getRange(cursor, cursor + num);
		cursor += num;
		return thisBatch;
	}

	/**
	 * Returns the number of input columns.
	 *
	 * @return the number of input columns
	 */
	@Override public int inputColumns() {
		return data.get(0).getFeatures().columns();
	}

	/**
	 * Returns the total number of labels.
	 *
	 * @return the total number of labels
	 */
	@Override public int totalOutcomes() {

		return data.get(0).getLabels().columns();
	}

	/**
	 * Resets the cursor.
	 */
	@Override public void reset() {
		cursor = 0;
	}

	/**
	 * Whether the iterator can be reset.
	 *
	 * @return true
	 */
	@Override public boolean resetSupported() {
		return true;
	}

	/**
	 * Whether the iterator can be used asynchronously.
	 *
	 * @return false
	 */
	@Override public boolean asyncSupported() {
		return false;
	}

	/**
	 * The size of the mini batches.
	 *
	 * @return the size of the mini batches
	 */
	@Override public int batch() {
		return batchSize;
	}

	/**
	 * Gets the preprocessor.
	 *
	 * @return preProcessor
	 */
	@Override public DataSetPreProcessor getPreProcessor() {
		return preProcessor;
	}

	/**
	 * Sets the preprocessor.
	 *
	 * @param preProcessor A DataSet preprocessor.
	 */
	@Override public void setPreProcessor(DataSetPreProcessor preProcessor) {
		this.preProcessor = preProcessor;
	}

	/**
	 * Gets the labels.
	 *
	 * @return the labels
	 */
	@Override public List<String> getLabels() {
		INDArray labelsINDArray = this.data.getLabels();
		List<String> labels = new ArrayList<>();
		for (int i = 0; i < labelsINDArray.shape()[0]; i++) {
			int label = labelsINDArray.getRow(i).argMax().getInt(0);
			labels.add(String.valueOf(label));
		}
		return labels;
	}

	/**
	 * Enables removing of a mini-batch.
	 */
	@Override public void remove() {
		throw new UnsupportedOperationException();
	}
}
