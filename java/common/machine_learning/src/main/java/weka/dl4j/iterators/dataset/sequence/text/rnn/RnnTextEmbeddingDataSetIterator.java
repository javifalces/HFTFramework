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
 * RnnTextEmbeddingDataSetIterator.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.dataset.sequence.text.rnn;

import static org.nd4j.linalg.indexing.NDArrayIndex.all;
import static org.nd4j.linalg.indexing.NDArrayIndex.interval;
import static org.nd4j.linalg.indexing.NDArrayIndex.point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.log4j.Log4j2;
import org.deeplearning4j.iterator.LabeledSentenceProvider;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import weka.core.Instances;
import weka.core.stopwords.AbstractStopwords;
import weka.dl4j.text.tokenization.preprocessor.TokenPreProcess;
import weka.dl4j.text.tokenization.tokenizer.factory.TokenizerFactory;

/**
 * A DataSetIterator implementation that reads text documents from an arff file and translates each
 * document to a sequence of wordvectors, given a wordvector model.
 *
 * @author Steven Lang
 */
@Log4j2 public class RnnTextEmbeddingDataSetIterator implements DataSetIterator, Serializable {

	private static final long serialVersionUID = 1682821361704251554L;
	protected final Instances data;
	private final WordVectors wordVectors;
	private final int batchSize;
	private final int wordVectorSize;
	private final int truncateLength;
	private final TokenizerFactory tokenizerFactory;
	protected AbstractStopwords stopWords;
	protected int cursor = 0;
	private LabeledSentenceProvider sentenceProvider;

	/**
	 * Constructor with necessary objects to create RNN features.
	 *
	 * @param data           Instances with documents and labels
	 * @param wordVectors    WordVectors object
	 * @param tokenFact      Tokenizer factory
	 * @param tpp            Token pre processor
	 * @param stopWords      Stop word object
	 * @param batchSize      Size of each minibatch for training
	 * @param truncateLength If reviews exceed
	 */
	public RnnTextEmbeddingDataSetIterator(Instances data, WordVectors wordVectors, TokenizerFactory tokenFact,
			TokenPreProcess tpp, AbstractStopwords stopWords, LabeledSentenceProvider sentenceProvider, int batchSize,
			int truncateLength) {
		this.batchSize = batchSize;
		this.wordVectorSize = wordVectors.getWordVector(wordVectors.vocab().wordAtIndex(0)).length;
		this.data = data;
		this.wordVectors = wordVectors;
		this.truncateLength = truncateLength;
		this.tokenizerFactory = tokenFact;
		this.tokenizerFactory.getBackend().setTokenPreProcessor(tpp.getBackend());
		this.stopWords = stopWords;
		this.sentenceProvider = sentenceProvider;
	}

	@Override public DataSet next(int num) {
		// Check if next() call is valid - throws appropriate exceptions
		checkIfNextIsValid();

		// Collect
		List<String> sentences = new ArrayList<>(num);
		List<Double> labelsRaw = new ArrayList<>(num);
		collectData(num, sentences, labelsRaw);
		final int numDocuments = sentences.size();

		// Tokenize sentences
		List<List<String>> tokenizedSentences = tokenizeSentences(sentences);

		// Get longest sentence length
		int maxSentenceLength = tokenizedSentences.stream().mapToInt(List::size).max().getAsInt();

		// Truncate maximum sentence length
		if (maxSentenceLength > truncateLength || maxSentenceLength == 0) {
			maxSentenceLength = truncateLength;
		}

		// Init feature/label arrays
		int[] featureShape = { numDocuments, wordVectorSize, maxSentenceLength };
		int[] labelShape = { numDocuments, data.numClasses(), maxSentenceLength };
		INDArray features = Nd4j.create(featureShape, 'f');
		INDArray labels = Nd4j.create(labelShape, 'f');
		INDArray featuresMask = Nd4j.zeros(numDocuments, maxSentenceLength);
		INDArray labelsMask = Nd4j.zeros(numDocuments, maxSentenceLength);

		for (int i = 0; i < numDocuments; i++) {
			List<String> tokens = tokenizedSentences.get(i);

			// Check for empty document
			if (tokens.isEmpty()) {
				continue;
			}

			// Get the last index of the current document (truncated)
			int lastIdx = Math.min(tokens.size(), maxSentenceLength);

			// Get all wordvectors in batch
			List<String> truncatedTokenList = tokens.subList(0, lastIdx);
			final INDArray vectors = wordVectors.getWordVectors(truncatedTokenList).transpose();

			/*
			 * Put wordvectors into features array at the following indices:
			 * 1) Document (i)
			 * 2) All vector elements which is equal to NDArrayIndex.interval(0, vectorSize)
			 * 3) All elements between 0 and the length of the current sequence
			 */
			INDArrayIndex[] indices = { point(i), all(), interval(0, lastIdx) };
			features.put(indices, vectors);

			// Assign "1" to each position where a feature is present, that is, in the interval of
			// [0, lastIdx)
			featuresMask.get(point(i), interval(0, lastIdx)).assign(1);

			// Put the labels in the labels and labelsMask arrays
			// Differ between classification and regression task
			if (data.numClasses() == 1) { // Regression
				double val = labelsRaw.get(i);
				labels.putScalar(new int[] { i, 0, lastIdx - 1 }, val);
			} else if (data.numClasses() > 1) { // Classification
				// One-Hot-Encoded class
				int idx = labelsRaw.get(i).intValue();
				// Set label
				labels.putScalar(new int[] { i, idx, lastIdx - 1 }, 1.0);
			} else {
				throw new RuntimeException("Could not detect classification or regression task.");
			}

			// Set final timestep for this example to 1.0 to show that an output exists here
			int[] lastTimestepIndex = { i, lastIdx - 1 };
			labelsMask.putScalar(lastTimestepIndex, 1.0);
		}

		// Cache the dataset
		final DataSet ds = new DataSet(features, labels, featuresMask, labelsMask);

		// Move cursor
		cursor += ds.numExamples();
		return ds;
	}

	/**
	 * Tokenize the given sentences.
	 *
	 * @param sentences List of sentences to tokenize
	 * @return List of list of tokens
	 */
	protected List<List<String>> tokenizeSentences(List<String> sentences) {
		return sentences.stream().map(this::tokenizeSingleSentence).collect(Collectors.toList());
	}

	/**
	 * Tokenize single sentence. Uses {@link RnnTextEmbeddingDataSetIterator#tokenizerFactory} to
	 * create the tokens and filters based on whether the {@link RnnTextEmbeddingDataSetIterator#wordVectors}
	 * model contains the token and further filters based on the given {@link
	 * RnnTextEmbeddingDataSetIterator#stopWords}.
	 *
	 * @param sentence Sentence to be tokenized
	 * @return Tokenized sentence
	 */
	protected List<String> tokenizeSingleSentence(String sentence) {
		return tokenizerFactory.getBackend().create(sentence).getTokens().stream().filter(wordVectors::hasWord)
				.filter(t -> !stopWords.isStopword(t)).collect(Collectors.toList());
	}

	/**
	 * Collect data from sentence provider and store it in {@code sentences} and {@code labelsRaw}.
	 *
	 * @param num       Number of datapoints to collect from sentence provider
	 * @param sentences Empty sentences list
	 * @param labelsRaw Empty labels list
	 */
	protected void collectData(int num, List<String> sentences, List<Double> labelsRaw) {
		for (int i = 0; i < num && sentenceProvider.hasNext(); i++) {
			final Pair<String, String> next = sentenceProvider.nextSentence();
			sentences.add(next.getFirst());
			labelsRaw.add(Double.valueOf(next.getSecond()));
		}
	}

	/**
	 * Check if the next() call is valid.
	 */
	protected void checkIfNextIsValid() {
		if (sentenceProvider == null) {
			throw new UnsupportedOperationException("Cannot do next/hasNext without a sentence provider");
		}
		if (!hasNext()) {
			throw new NoSuchElementException("No next element");
		}
	}

	@Override public int inputColumns() {
		return wordVectorSize;
	}

	@Override public int totalOutcomes() {
		return data.numClasses();
	}

	@Override public void reset() {
		sentenceProvider.reset();
		cursor = 0;
	}

	public boolean resetSupported() {
		return true;
	}

	@Override public boolean asyncSupported() {
		return true;
	}

	@Override public int batch() {
		return batchSize;
	}

	public int numExamples() {
		return data.numInstances();
	}

	@Override public List<String> getLabels() {
		return IntStream.range(0, data.numClasses()).boxed().map(i -> data.classAttribute().value(i))
				.collect(Collectors.toList());
	}

	@Override public boolean hasNext() {
		return cursor < numExamples();
	}

	@Override public DataSet next() {
		return next(batchSize);
	}

	@Override public void remove() {
	}

	@Override public DataSetPreProcessor getPreProcessor() {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override public void setPreProcessor(DataSetPreProcessor preProcessor) {
		throw new UnsupportedOperationException();
	}
}
