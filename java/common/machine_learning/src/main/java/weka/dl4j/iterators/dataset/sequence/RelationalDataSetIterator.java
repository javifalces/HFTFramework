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
 * RelationalDataSetIterator.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.dataset.sequence;

import static org.nd4j.linalg.indexing.NDArrayIndex.all;
import static org.nd4j.linalg.indexing.NDArrayIndex.interval;
import static org.nd4j.linalg.indexing.NDArrayIndex.point;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import weka.core.Instance;
import weka.core.Instances;

/**
 * A DataSetIterator implementation that parses Instances with relational attributes.
 *
 * @author Steven Lang
 */
public class RelationalDataSetIterator implements DataSetIterator {

	private static final long serialVersionUID = 8353870921670443077L;

	/**
	 * Dataset
	 */
	protected final Instances data;
	/**
	 * Batch size
	 */
	protected final int batchSize;
	/**
	 * Maximum sequence length
	 */
	protected final int truncateLength;
	/**
	 * Number of features in the relational attribute
	 */
	protected final int numFeatures;
	/**
	 * Relational attribute index
	 */
	protected final int relationalAttributeIndex;
	/**
	 * Cursor to current row
	 */
	protected int cursor;

	/**
	 * Constructor.
	 *
	 * @param data                     Dataset
	 * @param batchSize                Batch size
	 * @param truncateLength           Maximum sequence length
	 * @param relationalAttributeIndex Relational attribute index
	 */
	public RelationalDataSetIterator(Instances data, int batchSize, int truncateLength, int relationalAttributeIndex) {
		this.data = data;
		this.batchSize = batchSize;
		this.cursor = 0;
		this.truncateLength = truncateLength;
		this.relationalAttributeIndex = relationalAttributeIndex;
		this.numFeatures = data.attribute(relationalAttributeIndex).relation().numAttributes();
	}

	@Override public DataSet next(int num) {
		List<Instances> currentBatch = new ArrayList<>(num);
		List<Double> lbls = new ArrayList<>(num);

		for (int i = 0; i < num && cursor + i < data.numInstances(); i++) {
			currentBatch.add(data.get(cursor + i).relationalValue(relationalAttributeIndex));
			lbls.add(data.get(cursor + i).classValue());
		}

		final int currentBatchSize = currentBatch.size();

		int maxLength = 0;
		for (Instances instances : currentBatch) {
			maxLength = Math.max(maxLength, instances.numInstances());
		}

		// If longest instance exceeds 'truncateLength': only take the first 'truncateLength' instances
		if (maxLength > truncateLength || maxLength == 0) {
			maxLength = truncateLength;
		}

		// Create data for training
		INDArray features = Nd4j.create(new int[] { currentBatchSize, numFeatures, maxLength }, 'f');
		INDArray labels = Nd4j.create(new int[] { currentBatchSize, data.numClasses(), maxLength }, 'f');

		// Because we are dealing with instances of different lengths and only one output at the final
		// time step: use padding arrays
		// Mask arrays contain 1 if data is present at that time step for that example, or 0 if data is
		// just padding
		INDArray featuresMask = Nd4j.zeros(currentBatchSize, maxLength);
		INDArray labelsMask = Nd4j.zeros(currentBatchSize, maxLength);

		for (int i = 0; i < currentBatchSize; i++) {
			Instances currInstances = currentBatch.get(i);

			// Check for empty row
			final int currNumInstances = currInstances.numInstances();
			if (currNumInstances == 0) {
				continue;
			}

			// Get the sequence length of row (i)
			int lastIdx = Math.min(currNumInstances, maxLength);

			// Matrix that will represent the current row/instances object
			INDArray currDataND = Nd4j.create(numFeatures, lastIdx);

			// Iterate over truncated number of instances for the current row
			for (int j = 0; j < lastIdx; j++) {
				// Get as double array
				final double[] doubles = currInstances.get(j).toDoubleArray();
				final INDArray indArray = Nd4j.create(doubles);
				currDataND.putColumn(j, indArray);
			}

			features.put(new INDArrayIndex[] { point(i), all(), interval(0, lastIdx) }, currDataND);

			// Assign "1" to each position where a feature is present, that is, in the interval of
			// [0, lastIdx)
			featuresMask.get(new INDArrayIndex[] { point(i), interval(0, lastIdx) }).assign(1);

      /*
       Put the labels in the labels and labelsMask arrays
      */

			// Differ between classification and regression task
			if (data.numClasses() == 1) { // Regression
				double val = lbls.get(i);
				labels.putScalar(new int[] { i, 0, lastIdx - 1 }, val);
			} else if (data.numClasses() > 1) { // Classification
				// One-Hot-Encoded class
				int idx = lbls.get(i).intValue();
				// Set label
				labels.putScalar(new int[] { i, idx, lastIdx - 1 }, 1.0);
			} else {
				throw new RuntimeException("Could not detect classification or regression task.");
			}

			// Specify that an output exists at the final time step for this example
			labelsMask.putScalar(new int[] { i, lastIdx - 1 }, 1.0);
		}

		// Cache the dataset
		final DataSet ds = new DataSet(features, labels, featuresMask, labelsMask);

		// Move cursor
		cursor += ds.numExamples();
		return ds;
	}

	@Override public int inputColumns() {
		return numFeatures;
	}

	@Override public int totalOutcomes() {
		return data.numClasses();
	}

	@Override public boolean resetSupported() {
		return true;
	}

	@Override public boolean asyncSupported() {
		return true;
	}

	@Override public void reset() {
		cursor = 0;
	}

	@Override public int batch() {
		return batchSize;
	}

	public int numExamples() {
		return data.numInstances();
	}

	@Override public DataSetPreProcessor getPreProcessor() {
		return null;
	}

	@Override public void setPreProcessor(DataSetPreProcessor preProcessor) {
	}

	@Override public List<String> getLabels() {
		return data.stream().map(Instance::classValue).map(String::valueOf).distinct().sorted()
				.collect(Collectors.toList());
	}

	@Override public boolean hasNext() {
		return cursor < numExamples();
	}

	@Override public DataSet next() {
		return next(batchSize);
	}
}
