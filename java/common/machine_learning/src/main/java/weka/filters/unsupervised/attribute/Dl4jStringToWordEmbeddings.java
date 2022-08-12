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
 * Dl4jStringToWordEmbeddings.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.deeplearning4j.models.sequencevectors.SequenceVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.dl4j.text.stopwords.Dl4jAbstractStopwords;
import weka.dl4j.text.stopwords.Dl4jNull;
import weka.dl4j.text.tokenization.preprocessor.CommonPreProcessor;
import weka.dl4j.text.tokenization.preprocessor.TokenPreProcess;
import weka.dl4j.text.tokenization.tokenizer.factory.DefaultTokenizerFactory;
import weka.dl4j.text.tokenization.tokenizer.factory.TokenizerFactory;
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;

/**
 * <!-- globalinfo-start --> An abstract attribute filter that calculates word embeddings on a
 * String attribute. <!-- globalinfo-end -->
 *
 * @author Felipe Bravo-Marquez (fjb11@students.waikato.ac.nz)
 */
public abstract class Dl4jStringToWordEmbeddings extends SimpleBatchFilter {

	/**
	 * For serialization
	 */
	private static final long serialVersionUID = 3748678887246129719L;

	/**
	 * The object where word embeddings are stored
	 */
	protected SequenceVectors<VocabWord> vec;
	/**
	 * Prefix for embedding attributes
	 */
	protected String embeddingPrefix = "embedding-";
	/**
	 * Number of stopWords (from left to right) of the tweet whose embeddings will be concatenated.
	 */
	protected int concatWords = 15;
	/**
	 * Action to perform on the embeddings. This action will define whether word or document vectors
	 * are produced.
	 */
	protected Action action = Action.WORD_VECTOR;
	/**
	 * The TokenPreProcess object
	 */
	protected TokenPreProcess preprocessor = new CommonPreProcessor();
	/**
	 * The TokenizerFactory object
	 */
	protected TokenizerFactory tokenizerFactory = new DefaultTokenizerFactory();
	/**
	 * The Stopwords class
	 */
	protected Dl4jAbstractStopwords stopWordsHandler = new Dl4jNull();
	/**
	 * The number of epochs
	 */
	protected int epochs = 1;
	/**
	 * The maximum number of concurrent threads available for training.
	 */
	protected int workers = Runtime.getRuntime().availableProcessors();
	/**
	 * the index of the string attribute to be processed.
	 */
	protected int textIndex = 1;
	;
	/**
	 * the minimum frequency of a word to be considered.
	 */
	protected int minWordFrequency = 5;
	/**
	 * the layer size.
	 */
	protected int layerSize = 100;
	/**
	 * the number of iterations
	 */
	protected int iterations = 1;
	/**
	 * the size of the window.
	 */
	protected int windowSize = 5;
	/**
	 * Random number seed
	 */
	protected int seed = 1;

	/* (non-Javadoc)
	 * @see weka.filters.Filter#listOptions()
	 */
	@Override public Enumeration<Option> listOptions() {
		return Option.listOptionsForClass(this.getClass()).elements();
	}

	/* (non-Javadoc)
	 * @see weka.filters.Filter#getOptions()
	 */
	@Override public String[] getOptions() {
		return Option.getOptions(this, this.getClass());
	}

	/**
	 * Parses the options for this object.
	 *
	 * @param options the options to use
	 * @throws Exception if setting of options fails
	 */
	@Override public void setOptions(String[] options) throws Exception {
		Option.setOptionsForHierarchy(options, this, Filter.class);
	}

	/* (non-Javadoc)
	 * @see weka.filters.Filter#getCapabilities()
	 */
	@Override public Capabilities getCapabilities() {

		Capabilities result = new Capabilities(this);
		result.disableAll();

		// attributes
		result.enableAllAttributes();
		result.enable(Capability.MISSING_VALUES);

		// class
		result.enableAllClasses();
		result.enable(Capability.MISSING_CLASS_VALUES);
		result.enable(Capability.NO_CLASS);

		result.setMinimumNumberInstances(0);

		return result;
	}

	/* (non-Javadoc)
	 * @see weka.filters.SimpleFilter#determineOutputFormat(weka.core.Instances)
	 */
	@Override protected Instances determineOutputFormat(Instances inputFormat) throws Exception {

		ArrayList<Attribute> att = new ArrayList<Attribute>();

		if (this.action.equals(Action.WORD_VECTOR)) {
			// Add one attribute for each embedding dimension
			for (int i = 0; i < this.layerSize; i++) {
				att.add(new Attribute(embeddingPrefix + i));
			}

			att.add(new Attribute("word_id", (ArrayList<String>) null));

			Instances result = new Instances("Word vectors calculated from:" + inputFormat.relationName(), att, 0);

			return result;

		}

		// Represent doc vectors
		else {
			// Adds all attributes of the inputformat
			for (int i = 0; i < inputFormat.numAttributes(); i++) {
				att.add(inputFormat.attribute(i));
			}
			if (this.action.equals(Action.DOC_VECTOR_ADD) || this.action.equals(Action.DOC_VECTOR_AVERAGE)) {
				for (int i = 0; i < this.layerSize; i++) {
					att.add(new Attribute(embeddingPrefix + i));
				}
			} else if (this.action.equals(Action.DOC_VECTOR_CONCAT)) {
				for (int i = 0; i < this.concatWords; i++) {
					for (int j = 0; j < this.layerSize; j++) {
						att.add(new Attribute(embeddingPrefix + i + "," + j));
					}
				}
			}

			Instances result = new Instances(inputFormat.relationName(), att, 0);

			// set the class index
			result.setClassIndex(inputFormat.classIndex());

			return result;
		}
	}

	/**
	 * Calculates word embeddings from Weka Instances
	 *
	 * @param instances the Weka Instances object
	 */
	abstract void initiliazeVectors(Instances instances);

	/* (non-Javadoc)
	 * @see weka.filters.SimpleFilter#process(weka.core.Instances)
	 */
	@Override protected Instances process(Instances instances) throws Exception {

		Instances result = getOutputFormat();
		ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			result = doProcess(instances);
		} finally {
			Thread.currentThread().setContextClassLoader(origLoader);
		}

		return result;
	}

	/**
	 * Do the actual process step.
	 *
	 * @param instances Input
	 * @return Output
	 * @throws IOException Something went wrong.
	 */
	protected Instances doProcess(Instances instances) throws IOException {
		Instances result = getOutputFormat();
		if (this.textIndex > instances.numAttributes()) {
			throw new IOException("Invalid attribute index.");
		}

		if (!instances.attribute(this.textIndex - 1).isString()) {
			throw new IOException("Given attribute is not String.");
		}

		// create Embeddings in the first batch
		if (!isFirstBatchDone()) {
			this.initiliazeVectors(instances);
		}

		// outputs the word vectors
		if (this.action.equals(Action.WORD_VECTOR)) {

			String[] words = this.vec.getVocab().words().toArray(new String[0]);
			Arrays.sort(words);

			for (String word : words) {
				double[] values = new double[result.numAttributes()];

				for (int i = 0; i < this.vec.getWordVector(word).length; i++) {
					values[i] = this.vec.getWordVector(word)[i];
				}

				values[result.numAttributes() - 1] = result.attribute("word_id").addStringValue(word);

				Instance inst = new DenseInstance(1, values);

				inst.setDataset(result);
				result.add(inst);
			}

		}

		// outputs doc vectors using the embeddings
		else {
			// reference to the content of the message, users index start from zero
			Attribute attrCont = instances.attribute(this.textIndex - 1);

			// copy all previous attributes
			for (int i = 0; i < instances.numInstances(); i++) {
				double[] values = new double[result.numAttributes()];
				for (int n = 0; n < instances.numAttributes(); n++) {
					values[n] = instances.instance(i).value(n);
				}

				String content = instances.instance(i).stringValue(attrCont);
				List<String> words = this.tokenizerFactory.getBackend().create(content).getTokens();

				int m = 0;
				for (String word : words) {
					if (this.vec.hasWord(word)) {
						int j = 0;
						for (double embDimVal : this.vec.getWordVector(word)) {
							if (this.action == Action.DOC_VECTOR_AVERAGE) {
								values[result.attribute(embeddingPrefix + j).index()] += embDimVal / words.size();
							} else if (this.action == Action.DOC_VECTOR_ADD) {
								values[result.attribute(embeddingPrefix + j).index()] += embDimVal;
							} else if (this.action == Action.DOC_VECTOR_CONCAT) {
								if (m < this.concatWords) {
									values[result.attribute(embeddingPrefix + m + "," + j).index()] += embDimVal;
								}
							}

							j++;
						}
					}
					m++;
				}

				Instance inst = new DenseInstance(1, values);

				inst.setDataset(result);

				// copy possible strings, relational values...
				copyValues(inst, false, instances, result);

				result.add(inst);
			}
		}
		return result;
	}

	public int getConcatWords() {
		return concatWords;
	}

	public void setConcatWords(int concatWords) {
		this.concatWords = concatWords;
	}

	@OptionMetadata(displayName = "action", description =
			"The action to perform on the embeddings: 1) report embeddings (WORD_VECTOR), "
					+ "2) Average embeddings of the input string (DOC_VECTOR_AVERAGE),"
					+ "3) Add embeddings of the input string (DOC_VECTOR_ADD), "
					+ "4) Concatenate the first *concat_words* embeddings of  the input string (DOC_VECTOR_CONCAT), (default WORD_VECTOR).", commandLineParamName = "action", commandLineParamSynopsis = "-level <speficiation>", displayOrder = 1) public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	@OptionMetadata(displayName = "concat_words", description =
			"Number of stopWords (from left to right) of the tweet whose embeddings will be concatenated."
					+ "This parameter only applies if action=DOC_VECTOR_CONCAT (default = 15).", commandLineParamName = "concat_words", commandLineParamSynopsis = "-concat_words <int>", displayOrder = 2) public int getConcat_words() {
		return concatWords;
	}

	public void setConcat_words(int m_concat_words) {
		this.concatWords = m_concat_words;
	}

	@OptionMetadata(displayName = "stopWordsHandler", description = "The stopWordsHandler. Dl4j Null means no stop stopWords are used.", commandLineParamName = "stopWordsHandler", commandLineParamSynopsis = "-stopWordsHandler <String>", displayOrder = 3) public Dl4jAbstractStopwords getStopWordsHandler() {
		return stopWordsHandler;
	}

	public void setStopWordsHandler(Dl4jAbstractStopwords m_stopWordsHandler) {
		this.stopWordsHandler = m_stopWordsHandler;
	}

	@OptionMetadata(displayName = "tokenizerFactory", description = "The tokenizer factory to use on the strings. Default: DefaultTokenizer.", commandLineParamName = "tokenizerFactory", commandLineParamSynopsis = "-tokenizerFactory <String>", displayOrder = 4) public TokenizerFactory getTokenizerFactory() {
		return tokenizerFactory;
	}

	public void setTokenizerFactory(TokenizerFactory m_tokenizerFactory) {
		this.tokenizerFactory = m_tokenizerFactory;
	}

	@OptionMetadata(displayName = "preprocessor", description = "The token Preprocessor for preprocessing the Strings. Default: CommonPreProcessor.", commandLineParamName = "preprocessor", commandLineParamSynopsis = "-preprocessor <String>", displayOrder = 5)

	/**
	 * Gets the action for the selected preprocessor.
	 *
	 * @return the current action.
	 */ public TokenPreProcess getPreProcessor() {
		return this.preprocessor;
	}

	/**
	 * Sets the preprocessor action.
	 *
	 * @param value the action type
	 */
	public void setPreProcessor(TokenPreProcess value) {
		this.preprocessor = value;
	}

	@OptionMetadata(displayName = "attribute string index", description = "The attribute string index (starting from 1) to process (default = 1).", commandLineParamName = "index", commandLineParamSynopsis = "-index <int>", displayOrder = 6)
	/**
	 * Get the position of the target string.
	 *
	 * @return the index of the target string
	 */ public int getTextIndex() {
		return textIndex;
	}

	/**
	 * Set the attribute's index with the string to process.
	 *
	 * @param textIndex the index value name
	 */
	public void setTextIndex(int textIndex) {
		this.textIndex = textIndex;
	}

	@OptionMetadata(displayName = "minWordFrequency", description = "The minimum word frequency (default = 5).", commandLineParamName = "minWordFrequency", commandLineParamSynopsis = "-minWordFrequency <int>", displayOrder = 7) public int getMinWordFrequency() {
		return minWordFrequency;
	}

	public void setMinWordFrequency(int minWordFrequency) {
		this.minWordFrequency = minWordFrequency;
	}

	@OptionMetadata(displayName = "layerSize", description = "The size of the word vectors (default = 100).", commandLineParamName = "layerSize", commandLineParamSynopsis = "-layerSize <int>", displayOrder = 8) public int getLayerSize() {
		return layerSize;
	}

	public void setLayerSize(int layerSize) {
		this.layerSize = layerSize;
	}

	@OptionMetadata(displayName = "iterations", description = "The number of iterations (default = 1).", commandLineParamName = "iterations", commandLineParamSynopsis = "-iterations <int>", displayOrder = 9) public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	@OptionMetadata(displayName = "windowSize", description = "The size of the window (default = 5).", commandLineParamName = "windowSize", commandLineParamSynopsis = "-windowSize <int>", displayOrder = 10) public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	@OptionMetadata(displayName = "epochs", description = "The number of epochs (iterations over whole training corpus) for training (default = 1).", commandLineParamName = "epochs", commandLineParamSynopsis = "-epochs <int>", displayOrder = 11) public int getEpochs() {
		return epochs;
	}

	public void setEpochs(int m_epochs) {
		this.epochs = m_epochs;
	}

	@OptionMetadata(displayName = "workers", description = "The maximum number of concurrent threads available for training.", commandLineParamName = "workers", commandLineParamSynopsis = "-workers <int>", displayOrder = 12) public int getWorkers() {
		return workers;
	}

	public void setWorkers(int m_workers) {
		this.workers = m_workers;
	}

	@OptionMetadata(displayName = "seed", description = "The random number seed to be used. (default = 1).", commandLineParamName = "seed", commandLineParamSynopsis = "-seed <int>", displayOrder = 13) public int getSeed() {
		return seed;
	}

	public void setSeed(int m_seed) {
		this.seed = m_seed;
	}

	@OptionMetadata(displayName = "embedding_prefix", description = "The prefix for each embedding attribute. Default: \"embedding-\".", commandLineParamName = "embedding_prefix", commandLineParamSynopsis = "-embedding_prefix <String>", displayOrder = 14) public String getEmbedding_prefix() {
		return embeddingPrefix;
	}

	public void setEmbedding_prefix(String embeddingPrefix) {
		this.embeddingPrefix = embeddingPrefix;
	}

	/**
	 * Possible actions to perform on the embeddings.
	 */
	protected enum Action {
		WORD_VECTOR, DOC_VECTOR_AVERAGE, DOC_VECTOR_ADD, DOC_VECTOR_CONCAT
	}
}
