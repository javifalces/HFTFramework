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
 * Dl4jStringToWord2Vec.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.util.Enumeration;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.dl4j.text.sentenceiterator.WekaInstanceSentenceIterator;

/**
 * <!-- globalinfo-start --> An attribute filter that calculates word embeddings on a String
 * attribute using the Word2vec implementation provided by DeepLearning4j. <!-- globalinfo-end -->
 * <!-- technical-bibtex-start --> BibTeX:
 *
 * <pre>
 * &#64;@Article{Word2Vec,
 *  Title                    = {Efficient estimation of word representations in vector space.},
 *  Author                   = {Mikolov, Tomas and Chen, Kai and Corrado, Greg and Dean, Jeffrey},
 *  Journal                  = {arXiv preprint arXiv:1301.3781},
 *  Year                     = {2013}
 * }
 *
 *
 *
 * </pre>
 * <p>
 * <!-- technical-bibtex-end -->
 *
 * @author Felipe Bravo-Marquez (fjb11@students.waikato.ac.nz)
 */
public class Dl4jStringToWord2Vec extends Dl4jStringToWordEmbeddings {

	/**
	 * For serialization
	 */
	private static final long serialVersionUID = -1767367935663656698L;

	/**
	 * The learning rate
	 */
	protected double learningRate = 0.025;

	/**
	 * The minimum learning rate
	 */
	protected double minLearningRate = 0.0001;

	/**
	 * True for using adaptive gradients
	 */
	protected boolean useAdaGrad = false;

	/**
	 * The mini batch size
	 */
	protected int batchSize = 512;

	/**
	 * The negativeSamplingValue sampling value for skip-gram algorithm.
	 */
	protected double negativeSamplingValue = 0.0d;

	/**
	 * Enable/disable hierarchic softmax
	 */
	protected boolean useHierarchicSoftmax = true;

	/**
	 * The sub-sampling threshold.
	 */
	protected double subSamplingThres = 0.0d;

	/**
	 * Enables/disables parallel tokenization.
	 */
	protected boolean allowParallelTokenization = true;

	/**
	 * Enables/disables periodical vocab truncation during construction.
	 */
	protected boolean enableScavenger = false;

	/**
	 * Returns a string describing this filter.
	 *
	 * @return a description of the filter suitable for displaying in the explorer/experimenter gui
	 */
	@Override public String globalInfo() {
		return "Calculates word embeddings on a string attribute using the Word2Vec method.\n"
				+ "More info at: https://code.google.com/archive/p/word2vec/ .\n" + getTechnicalInformation()
				.toString();
	}

	/**
	 * Returns an instance of a TechnicalInformation object, containing detailed information about the
	 * technical background of this class, e.g., paper reference or book this class is based on.
	 *
	 * @return the technical information about this class
	 */
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation result;
		result = new TechnicalInformation(Type.ARTICLE);
		result.setValue(TechnicalInformation.Field.AUTHOR, "Tomas Mikolov, Kai Chen, Greg Corrado, and Jeffrey Dean");
		result.setValue(TechnicalInformation.Field.TITLE,
				"Efficient Estimation of Word Representations in Vector Space");
		result.setValue(TechnicalInformation.Field.YEAR, "2013");
		result.setValue(TechnicalInformation.Field.BOOKTITLE, "In Proceedings of Workshop at ICLR.");

		return result;
	}

	/* (non-Javadoc)
	 * @see weka.filters.Filter#listOptions()
	 */
	@Override public Enumeration<Option> listOptions() {
		// this.getClass().getSuperclass()
		return Option.listOptionsForClassHierarchy(this.getClass(), this.getClass().getSuperclass()).elements();
	}

	/* (non-Javadoc)
	 * @see weka.filters.Filter#getOptions()
	 */
	@Override public String[] getOptions() {
		return Option.getOptionsForHierarchy(this, this.getClass().getSuperclass());

		// return Option.getOptions(this, this.getClass());
	}

	/**
	 * Parses the options for this object.
	 *
	 * @param options the options to use
	 * @throws Exception if setting of options fails
	 */
	@Override public void setOptions(String[] options) throws Exception {
		Option.setOptionsForHierarchy(options, this, this.getClass().getSuperclass());
		// Option.setOptions(options, this, this.getClass());
	}

	/* (non-Javadoc)
	 * @see Dl4jStringToWordEmbeddings#initiliazeVectors(weka.core.Instances)
	 */
	@Override void initiliazeVectors(Instances instances) {
		SentenceIterator iter = new WekaInstanceSentenceIterator(instances, this.textIndex - 1);

		// sets the tokenizer
		this.tokenizerFactory.getBackend().setTokenPreProcessor(this.preprocessor.getBackend());

		// initializes stopwords
		this.stopWordsHandler.initialize();

		// Building model
		this.vec = new Word2Vec.Builder().minWordFrequency(this.minWordFrequency).useAdaGrad(this.useAdaGrad)
				.allowParallelTokenization(this.allowParallelTokenization).enableScavenger(this.enableScavenger)
				.negativeSample(this.negativeSamplingValue).sampling(this.subSamplingThres).epochs(this.epochs)
				.learningRate(this.learningRate).minLearningRate(this.minLearningRate).workers(this.workers)
				.iterations(this.iterations).layerSize(this.layerSize).seed(this.seed).windowSize(this.windowSize)
				.iterate(iter).stopWords(this.stopWordsHandler.getStopList())
				.tokenizerFactory(this.tokenizerFactory.getBackend()).build();

		// fit model
		this.vec.fit();
	}

	@OptionMetadata(displayName = "batchSize", description = "The mini-batch size (default = 512).", commandLineParamName = "batchSize", commandLineParamSynopsis = "-batchSize <int>", displayOrder = 15) public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int m_batchSize) {
		this.batchSize = m_batchSize;
	}

	@OptionMetadata(displayName = "learningRate", description = "The learning rate (default = 0.025).", commandLineParamName = "learningRate", commandLineParamSynopsis = "-learningRate <double>", displayOrder = 16) public double getLearningRate() {
		return learningRate;
	}

	public void setLearningRate(double m_learningRate) {
		this.learningRate = m_learningRate;
	}

	@OptionMetadata(displayName = "minLearningRate", description = "This method defines minimal learning rate value for training (default = 1.0E-4).", commandLineParamName = "minLearningRate", commandLineParamSynopsis = "-minLearningRate <double>", displayOrder = 17) public double getMinLearningRate() {
		return minLearningRate;
	}

	public void setMinLearningRate(double m_minLearningRate) {
		this.minLearningRate = m_minLearningRate;
	}

	@OptionMetadata(displayName = "useAdaGrad", description = "This method defines whether adaptive gradients should be used or not (default = false).", commandLineParamName = "useAdaGrad", commandLineParamSynopsis = "-useAdaGrad", commandLineParamIsFlag = true, displayOrder = 18) public boolean isUseAdaGrad() {
		return useAdaGrad;
	}

	public void setUseAdaGrad(boolean m_useAdaGrad) {
		this.useAdaGrad = m_useAdaGrad;
	}

	@OptionMetadata(displayName = "negative", description = "The negative sampling value for skip-gram algorithm (default = 0.0).", commandLineParamName = "negative", commandLineParamSynopsis = "-negative <double>", displayOrder = 19) public double getNegative() {
		return negativeSamplingValue;
	}

	public void setNegative(double m_negative) {
		this.negativeSamplingValue = m_negative;
	}

	@OptionMetadata(displayName = "useHierarchicSoftmax", description = "Enable/disable hierarchic softmax (default = true).", commandLineParamName = "useHierarchicSoftmax", commandLineParamSynopsis = "-useHierarchicSoftmax", commandLineParamIsFlag = true, displayOrder = 20) public boolean isUseHierarchicSoftmax() {
		return useHierarchicSoftmax;
	}

	public void setUseHierarchicSoftmax(boolean m_useHierarchicSoftmax) {
		this.useHierarchicSoftmax = m_useHierarchicSoftmax;
	}

	@OptionMetadata(displayName = "sampling", description = "The sub-sampling threshold (default = 0.0).", commandLineParamName = "sampling", commandLineParamSynopsis = "-sampling <double>", displayOrder = 21) public double getSampling() {
		return subSamplingThres;
	}

	public void setSampling(double m_sampling) {
		this.subSamplingThres = m_sampling;
	}

	@OptionMetadata(displayName = "allowParallelTokenization", description = "Enables/disables parallel tokenization (default = true).", commandLineParamName = "allowParallelTokenization", commandLineParamSynopsis = "-allowParallelTokenization", commandLineParamIsFlag = true, displayOrder = 22) public boolean isAllowParallelTokenization() {
		return allowParallelTokenization;
	}

	public void setAllowParallelTokenization(boolean m_allowParallelTokenization) {
		this.allowParallelTokenization = m_allowParallelTokenization;
	}

	@OptionMetadata(displayName = "enableScavenger", description = "Enables/disables periodical vocab truncation during construction (default = false).", commandLineParamName = "enableScavenger", commandLineParamSynopsis = "-enableScavenger", commandLineParamIsFlag = true, displayOrder = 23) public boolean isEnableScavenger() {
		return enableScavenger;
	}

	public void setEnableScavenger(boolean m_enableScavenger) {
		this.enableScavenger = m_enableScavenger;
	}
}
