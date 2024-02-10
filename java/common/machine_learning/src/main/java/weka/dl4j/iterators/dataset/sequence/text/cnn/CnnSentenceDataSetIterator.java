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
 * CnnSentenceDataSetIterator.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.dataset.sequence.text.cnn;

import static org.nd4j.linalg.indexing.NDArrayIndex.all;
import static org.nd4j.linalg.indexing.NDArrayIndex.interval;
import static org.nd4j.linalg.indexing.NDArrayIndex.point;
import static weka.dl4j.Utils.getFieldValue;
import static weka.dl4j.Utils.invokeMethod;
import static weka.dl4j.Utils.setFieldValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.deeplearning4j.iterator.LabeledSentenceProvider;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import weka.core.stopwords.AbstractStopwords;
import weka.dl4j.Utils;

/**
 * CnnSentenceDataSetIterator extension to Deeplearning4j implementation. This class extends its
 * parent {@link org.deeplearning4j.iterator.CnnSentenceDataSetIterator#next(int)} method to support
 * regression with numeric values.
 * <p>
 * Since {@link org.deeplearning4j.iterator.CnnSentenceDataSetIterator} only has private access to
 * its fields and most of its methods, it is necessary to make use of
 * <p>
 * - {@link Utils#getFieldValue(Object, String)}, - {@link
 * Utils#setFieldValue(Object, String, Object)} - {@link
 * Utils#invokeMethod(Object, String, Object...)}
 * <p>
 * until field/method visibility is changed upstream.
 *
 * @author Steven Lang
 */
public class CnnSentenceDataSetIterator extends org.deeplearning4j.iterator.CnnSentenceDataSetIterator {

	private static final long serialVersionUID = 685006779872000151L;

	/**
	 * Stop words.
	 */
	private AbstractStopwords stopwords;

	/**
	 * Constructor that uses {@link Builder} extended with stopwords.
	 *
	 * @param builder Builder
	 */
	protected CnnSentenceDataSetIterator(Builder builder) {
		super(builder);
		this.stopwords = builder.stopwords;
		setUnknownWordHandling(UnknownWordHandling.UseUnknownVector);

		// Set unknown word
		WordVectors wordVectors = getWordVectors();
		wordVectors.setUNK("UNKNOWN");

		// Initialize unknown word manually
		INDArray unknown;
		if (getUseNormalizedWordVectors()) {
			unknown = wordVectors.getWordVectorMatrixNormalized(wordVectors.getUNK());
		} else {
			unknown = wordVectors.getWordVectorMatrix(wordVectors.getUNK());
		}
		setUnknown(unknown);
	}

	public static String getUnknownWordSentinel() {
		return "UNKNOWN_WORD_SENTINEL";
	}

	@Override public boolean hasNext() {
		LabeledSentenceProvider sentenceProvider = getSentenceProvider();
		if (sentenceProvider == null) {
			throw new RuntimeException("Sentenceprovider was null");
		}
		return sentenceProvider.hasNext();
	}

	@Override public DataSet next(int num) {
		if (!hasNext()) {
			// This should not happen as hasNext() should always be called prior to next(..)
			throw new NoSuchElementException("No next element available");
		}

		// Get the data for this mini batch
		List<Datum> data = collectData(num);

		// Get min and max token sizes in the current mini batch
		int maxTokenSizeBatch = Integer.MIN_VALUE;
		int minTokenSizeBatch = Integer.MAX_VALUE;
		if (data.size() > 0) {
			maxTokenSizeBatch = data.stream().mapToInt(Datum::numTokens).max().getAsInt();
			minTokenSizeBatch = data.stream().mapToInt(Datum::numTokens).min().getAsInt();
		}

		// Clip maxTokenSizeBatch with tokenLimit as upper bound
		int tokenLimit = getMaxSentenceLength();
		if (tokenLimit > 0) {
			maxTokenSizeBatch = Math.min(maxTokenSizeBatch, tokenLimit);
		}

		// Create labels
		INDArray labels = createLabels(data);

		// Create features
		INDArray features = createFeatures(data, maxTokenSizeBatch);

		// Create feature mask if token sizes vary
		INDArray featuresMask = null;
		boolean hasDifferentTokenSizes = minTokenSizeBatch != maxTokenSizeBatch;
		if (hasDifferentTokenSizes) {
			featuresMask = createFeatureMask(data, maxTokenSizeBatch);
		}

		// Create DataSet
		DataSet dataSet = new DataSet(features, labels, featuresMask, null);

		// Preprocess DataSet
		applyPreProcessing(dataSet);

		// Increment the cursor
		incrementCursor(dataSet.numExamples());
		return dataSet;
	}

	/**
	 * Collect the data (List of datapoints containing tokens and an associated label.
	 *
	 * @param num Requested number of data points
	 * @return List of datapoints
	 */
	protected List<Datum> collectData(int num) {
		LabeledSentenceProvider sentenceProvider = getSentenceProvider();
		String unknownWordSentinel = getUnknownWordSentinel();
		List<Datum> data = new ArrayList<>(num);
		for (int i = data.size(); i < num && sentenceProvider.hasNext(); i++) {
			Pair<String, String> p = sentenceProvider.nextSentence();
			String sentence = p.getFirst();
			String label = p.getSecond();
			List<String> tokens = tokenizeSentence(sentence);

			// Skip stopwords
			tokens = tokens.stream().filter(s -> !stopwords.isStopword(s)).collect(Collectors.toList());

			// If tokens are empty, add at least the UNKNOWN_WORD_SENTINEL
			if (tokens.isEmpty()) {
				tokens.add(unknownWordSentinel);
			}

			data.add(new Datum(tokens, label));
		}
		return data;
	}

	/**
	 * Create labels NDArray based on the input batch data
	 *
	 * @param data Input batch data
	 * @return NDArray containing the labels
	 */
	protected INDArray createLabels(List<Datum> data) {
		int numClasses = getNumClasses();
		INDArray labels = Nd4j.create(data.size(), numClasses);
		boolean isClassification = numClasses > 1;
		boolean isRegression = numClasses == 1;

		// Create labels based on task (Regression/Classification)
		if (isClassification) {
			createLabelsOneHot(data, labels);
		} else if (isRegression) {
			createLabelsRegression(data, labels);
		} else {
			throw new IllegalStateException("Number of classes must be >= 1.");
		}
		return labels;
	}

	/**
	 * Create labels from a regression task and store them in {@code labels}.
	 *
	 * @param data   Input batch data
	 * @param labels NDArray to store the labels in
	 */
	protected void createLabelsRegression(List<Datum> data, INDArray labels) {
		// Fix labels array for Regression task
		for (int i = 0; i < data.size(); i++) {
			final String labelStr = data.get(i).getLabel();
			double lbl = Double.parseDouble(labelStr);
			labels.putScalar(i, lbl);
		}
	}

	/**
	 * Create one-hot encoded labels for classification and store them in {@code labels}.
	 *
	 * @param data   Input batch data
	 * @param labels NDArray to store the labels in
	 */
	protected void createLabelsOneHot(List<Datum> data, INDArray labels) {
		Map<String, Integer> labelClassMap = getLabelClassMap();
		for (int i = 0; i < data.size(); i++) {
			String label = data.get(i).getLabel();
			if (!labelClassMap.containsKey(label)) {
				throw new IllegalStateException("Invalid label " + label + ".");
			}

			// Assign value 1.0 label at one-hot encoded label index
			int oneHotIndex = labelClassMap.get(label);
			labels.putScalar(i, oneHotIndex, 1.0);
		}
	}

	/**
	 * Create the features based on the data of this batch.
	 *
	 * @param data              Batch data
	 * @param maxTokenSizeBatch Maximum token size in this batch
	 * @return INDArray containing the features
	 */
	protected INDArray createFeatures(List<Datum> data, int maxTokenSizeBatch) {
		int tokenLimit = getMaxSentenceLength();

		// Determine feature shape
		int[] featuresShape = getFeatureShape(maxTokenSizeBatch, data.size());

		// Create features from tokens
		INDArray features = Nd4j.create(featuresShape);
		for (int i = 0; i < data.size(); i++) {
			List<String> currSentence = data.get(i).getTokens();
			for (int j = 0; j < currSentence.size() && j < tokenLimit; j++) {
				String token = currSentence.get(j);
				INDArray vectorizedToken = getVector(token);
				INDArrayIndex[] indices = new INDArrayIndex[4];
				indices[0] = point(i);
				indices[1] = point(0);
				indices[2] = point(j);
				indices[3] = all();
				features.put(indices, vectorizedToken);
			}
		}
		return features;
	}

	/**
	 * Create the feature mask.
	 *
	 * @param data         Tokenized sentences with labels
	 * @param maxTokenSize Maximum token size
	 * @return Feature mask
	 */
	protected INDArray createFeatureMask(List<Datum> data, int maxTokenSize) {
		INDArray featuresMask = Nd4j.create(data.size(), 1, maxTokenSize, 1);
		INDArrayIndex[] indices = new INDArrayIndex[4];
		indices[1] = all();
		indices[3] = all();
		for (int i = 0; i < data.size(); i++) {
			indices[0] = point(i);
			int sentenceLength = data.get(i).numTokens();
			sentenceLength = Math.min(sentenceLength, maxTokenSize);
			indices[2] = interval(0, sentenceLength);
			featuresMask.put(indices, 1.0);
		}
		return featuresMask;
	}

	/**
	 * Apply the preprocessing step if preprocessor is available.
	 *
	 * @param dataSet DataSet
	 */
	protected void applyPreProcessing(DataSet dataSet) {
		DataSetPreProcessor preProcessor = getDataSetPreProcessor();
		if (preProcessor != null) {
			preProcessor.preProcess(dataSet);
		}
	}

	/**
	 * Get the feature shape.
	 *
	 * @param maxLength  Maximum token lenght
	 * @param numSamples Minibatch size
	 * @return Feature shape
	 */
	protected int[] getFeatureShape(int maxLength, int numSamples) {
		return new int[] { numSamples, 1, maxLength, getWordVectorSize() };
	}

	public LabeledSentenceProvider getSentenceProvider() {
		return getFieldValue(this, "sentenceProvider");
	}

	public int getMaxSentenceLength() {
		return getFieldValue(this, "maxSentenceLength");
	}

	public boolean isSentencesAlongHeight() {
		return getFieldValue(this, "sentencesAlongHeight");
	}

	public DataSetPreProcessor getDataSetPreProcessor() {
		return getFieldValue(this, "dataSetPreProcessor");
	}

	public int getWordVectorSize() {
		return getFieldValue(this, "wordVectorSize");
	}

	public int getNumClasses() {
		return getFieldValue(this, "numClasses");
	}

	public boolean getUseNormalizedWordVectors() {
		return getFieldValue(this, "useNormalizedWordVectors");
	}

	public void setUseNormalizedWordVectors(boolean value) {
		setFieldValue(this, "useNormalizedWordVectors", value);
	}

	public int getCursor() {
		return getFieldValue(this, "cursor");
	}

	public void setCursor(int value) {
		setFieldValue(this, "cursor", value);
	}

	public INDArray getUnknown() {
		return getFieldValue(this, "unknown");
	}

	public void setUnknown(INDArray value) {
		setFieldValue(this, "unknown", value);
	}

	public WordVectors getWordVectors() {
		return getFieldValue(this, "wordVectors");
	}

	public void setWordVectors(WordVectors value) {
		setFieldValue(this, "wordVectors", value);
	}

	public UnknownWordHandling getUnknownWordHandling() {
		return getFieldValue(this, "unknownWordHandling");
	}

	public void setUnknownWordHandling(UnknownWordHandling value) {
		setFieldValue(this, "unknownWordHandling", value);
	}

	protected void incrementCursor(int n) {
		int oldCurser = getCursor();
		setCursor(oldCurser + n);
	}

	public AbstractStopwords getStopwords() {
		return stopwords;
	}

	protected INDArray getVector(String s) {
		return invokeMethod(this, "getVector", s);
	}

	protected List<String> tokenizeSentence(String sentence) {
		return invokeMethod(this, "tokenizeSentence", sentence);
	}

	/**
	 * CnnSentenceDataSetIterator.Builder implementation that supports stopwords.
	 */
	public static class Builder extends org.deeplearning4j.iterator.CnnSentenceDataSetIterator.Builder {

		/**
		 * Stopwords
		 */
		AbstractStopwords stopwords;

		/**
		 * Set stopwords.
		 *
		 * @param stopwords Stopwords
		 * @return Builder instance
		 */
		public Builder stopwords(AbstractStopwords stopwords) {
			this.stopwords = stopwords;
			return this;
		}

		/**
		 * Build the iterator.
		 *
		 * @return {@link CnnSentenceDataSetIterator} instance
		 */
		public CnnSentenceDataSetIterator build() {
			return new CnnSentenceDataSetIterator(this);
		}
	}

	/**
	 * Simple data point of tokens with an associated label
	 */
	private class Datum {

		/**
		 * Tokens in this datum
		 */
		private List<String> tokens;

		/**
		 * Associated label with the tokens
		 */
		private String label;

		/**
		 * Constructor with tokens and label.
		 *
		 * @param tokens Tokens
		 * @param label  Label
		 */
		public Datum(List<String> tokens, String label) {
			this.tokens = tokens;
			this.label = label;
		}

		/**
		 * Get the number of tokens in this datum.
		 *
		 * @return Number of tokens in this datum
		 */
		public int numTokens() {
			return tokens.size();
		}

		/**
		 * Get the label.
		 *
		 * @return Label
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Get the tokens.
		 *
		 * @return Tokens
		 */
		public List<String> getTokens() {
			return tokens;
		}
	}
}
