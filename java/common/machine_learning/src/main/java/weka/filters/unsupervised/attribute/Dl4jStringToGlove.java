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
 * Dl4jStringToGlove.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.util.Enumeration;

import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Type;
import weka.dl4j.text.sentenceiterator.WekaInstanceSentenceIterator;

/**
 * <!-- globalinfo-start --> An attribute filter that calculates word embeddings on a String
 * attribute using the Glove implementation provided by DeepLearning4j. <!-- globalinfo-end --> <!--
 * technical-bibtex-start --> BibTeX:
 *
 * <pre>
 * &#64;@Article{Glove,
 *  Title                    = {Glove: Global Vectors for Word Representation.},
 *  Author                   = {Pennington, Jeffrey and Socher, Richard and Manning, Christopher D},
 *  Booktitle                = {EMNLP},
 *  Year                     = {2014}
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
public class Dl4jStringToGlove {
	// Deeplearning4j GLOVE model has been removed as per:
	// https://github.com/eclipse/deeplearning4j/commit/2cdb2b344704956098d97a422df8aacff384a024

	//public class Dl4jStringToGlove extends Dl4jStringToWordEmbeddings {
	//
	//  /**
	//   * For serialization
	//   */
	//  private static final long serialVersionUID = -1767367935663656698L;
	//
	//  /**
	//   * Parameters specifying, if cooccurrences list should be build into both directions from any
	//   * current word.
	//   */
	//  protected boolean symmetric = true;
	//
	//  /**
	//   * Parameter specifying, if cooccurrences list should be shuffled between training epochs.
	//   */
	//  protected boolean shuffle = true;
	//
	//  /**
	//   * Parameter specifying cutoff in weighting function; default 100.0
	//   */
	//  protected double xMax = 100.0;
	//
	//  /**
	//   * Parameter in exponent of weighting function
	//   */
	//  protected double alpha = 0.75;
	//
	//  /**
	//   * The learning rate
	//   */
	//  protected double learningRate = 0.05;
	//
	//  /**
	//   * The minimum learning rate
	//   */
	//  protected double minLearningRate = 0.0001;
	//
	//  /**
	//   * True for using adaptive gradients
	//   */
	//  protected boolean useAdaGrad = false;
	//
	//  /**
	//   * This parameter specifies batch size for each thread. Also, if shuffle == TRUE, this batch will
	//   * be shuffled before processing.
	//   */
	//  protected int batchSize = 1000;
	//
	//  /**
	//   * Returns a string describing this filter.
	//   *
	//   * @return a description of the filter suitable for displaying in the explorer/experimenter gui
	//   */
	//  @Override
	//  public String globalInfo() {
	//    return "Calculates word embeddings on a string attribute using the Glove method.\n"
	//        + "More info at: https://nlp.stanford.edu/projects/glove/ .\n"
	//        + getTechnicalInformation().toString();
	//  }
	//
	//  /**
	//   * Returns an instance of a TechnicalInformation object, containing detailed information about the
	//   * technical background of this class, e.g., paper reference or book this class is based on.
	//   *
	//   * @return the technical information about this class
	//   */
	//  public TechnicalInformation getTechnicalInformation() {
	//    TechnicalInformation result;
	//    result = new TechnicalInformation(Type.ARTICLE);
	//    result.setValue(
	//        TechnicalInformation.Field.AUTHOR,
	//        "Pennington, Jeffrey and Socher, Richard and Manning, Christopher D");
	//    result.setValue(
	//        TechnicalInformation.Field.TITLE, "Glove: Global Vectors for Word Representation");
	//    result.setValue(TechnicalInformation.Field.YEAR, "2014");
	//    result.setValue(TechnicalInformation.Field.BOOKTITLE, "EMNLP.");
	//
	//    return result;
	//  }
	//
	//  @Override
	//  public Enumeration<Option> listOptions() {
	//    // this.getClass().getSuperclass()
	//    return Option.listOptionsForClassHierarchy(this.getClass(), this.getClass().getSuperclass())
	//        .elements();
	//  }
	//
	//  /* (non-Javadoc)
	//   * @see weka.filters.Filter#getOptions()
	//   */
	//  @Override
	//  public String[] getOptions() {
	//    return Option.getOptionsForHierarchy(this, this.getClass().getSuperclass());
	//
	//    // return Option.getOptions(this, this.getClass());
	//  }
	//
	//  /**
	//   * Parses the options for this object.
	//   *
	//   * @param options the options to use
	//   * @throws Exception if setting of options fails
	//   */
	//  @Override
	//  public void setOptions(String[] options) throws Exception {
	//    Option.setOptionsForHierarchy(options, this, this.getClass().getSuperclass());
	//    // Option.setOptions(options, this, this.getClass());
	//  }
	//
	//  /* (non-Javadoc)
	//   * @see Dl4jStringToWordEmbeddings#initiliazeVectors(weka.core.Instances)
	//   */
	//  @Override
	//  void initiliazeVectors(Instances instances) {
	//    SentenceIterator iter = new WekaInstanceSentenceIterator(instances, this.textIndex - 1);
	//
	//    // sets the tokenizer
	//    this.tokenizerFactory.getBackend().setTokenPreProcessor(this.preprocessor.getBackend());
	//
	//    // initializes stopwords
	//    this.stopWordsHandler.initialize();
	//
	//    // Building model
	//    this.vec =
	//        new Glove.Builder()
	//            .iterate(iter)
	//            .tokenizerFactory(this.tokenizerFactory.getBackend())
	//            .alpha(this.alpha)
	//            .learningRate(this.learningRate)
	//            .epochs(this.epochs)
	//            .layerSize(this.layerSize)
	//            .minLearningRate(this.minLearningRate)
	//            .minWordFrequency(this.minWordFrequency)
	//            .stopWords(this.stopWordsHandler.getStopList())
	//            .useAdaGrad(this.useAdaGrad)
	//            .windowSize(this.windowSize)
	//            .workers(this.workers)
	//            .windowSize(this.windowSize)
	//            .xMax(this.xMax)
	//            .batchSize(this.batchSize)
	//            .shuffle(this.shuffle)
	//            .symmetric(this.symmetric)
	//            .build();
	//
	//    // fit model
	//    this.vec.fit();
	//  }
	//
	//  @OptionMetadata(
	//      displayName = "learningRate",
	//      description = "The learning rate (default = 0.05).",
	//      commandLineParamName = "learningRate",
	//      commandLineParamSynopsis = "-learningRate <double>",
	//      displayOrder = 15
	//  )
	//  public double getLearningRate() {
	//    return learningRate;
	//  }
	//
	//  public void setLearningRate(double m_learningRate) {
	//    this.learningRate = m_learningRate;
	//  }
	//
	//  @OptionMetadata(
	//      displayName = "minLearningRate",
	//      description =
	//          "This method defines minimal learning rate value for training (default = 1.0E-4).",
	//      commandLineParamName = "minLearningRate",
	//      commandLineParamSynopsis = "-minLearningRate <double>",
	//      displayOrder = 16
	//  )
	//  public double getMinLearningRate() {
	//    return minLearningRate;
	//  }
	//
	//  public void setMinLearningRate(double m_minLearningRate) {
	//    this.minLearningRate = m_minLearningRate;
	//  }
	//
	//  @OptionMetadata(
	//      displayName = "symmetric",
	//      description =
	//          "Parameters specifying, if cooccurrences list should be build into both directions from any current word (default = true).",
	//      commandLineParamName = "symmetric",
	//      commandLineParamSynopsis = "-symmetric",
	//      commandLineParamIsFlag = true,
	//      displayOrder = 17
	//  )
	//  public boolean isSymmetric() {
	//    return symmetric;
	//  }
	//
	//  public void setSymmetric(boolean m_symetric) {
	//    this.symmetric = m_symetric;
	//  }
	//
	//  @OptionMetadata(
	//      displayName = "shuffle",
	//      description =
	//          "Parameter specifying, if cooccurrences list should be shuffled between training epochs (default = true).",
	//      commandLineParamName = "shuffle",
	//      commandLineParamSynopsis = "-shuffle",
	//      commandLineParamIsFlag = true,
	//      displayOrder = 18
	//  )
	//  public boolean isShuffle() {
	//    return shuffle;
	//  }
	//
	//  public void setShuffle(boolean m_shuffle) {
	//    this.shuffle = m_shuffle;
	//  }
	//
	//  @OptionMetadata(
	//      displayName = "xMax",
	//      description = "Parameter specifying cutoff in weighting function (default = 100.0).",
	//      commandLineParamName = "xMax",
	//      commandLineParamSynopsis = "-xMax <double>",
	//      displayOrder = 19
	//  )
	//  public double getXMax() {
	//    return xMax;
	//  }
	//
	//  public void setXMax(double m_xMax) {
	//    this.xMax = m_xMax;
	//  }
	//
	//  @OptionMetadata(
	//      displayName = "alpha",
	//      description = "Parameter in exponent of weighting function (default = 0.75).",
	//      commandLineParamName = "alpha",
	//      commandLineParamSynopsis = "-alpha <double>",
	//      displayOrder = 20
	//  )
	//  public double getAlpha() {
	//    return alpha;
	//  }
	//
	//  public void setAlpha(double m_alpha) {
	//    this.alpha = m_alpha;
	//  }
	//
	//  @OptionMetadata(
	//      displayName = "useAdaGrad",
	//      description =
	//          "This method defines whether adaptive gradients should be used or not (default = false).",
	//      commandLineParamName = "useAdaGrad",
	//      commandLineParamSynopsis = "-useAdaGrad",
	//      commandLineParamIsFlag = true,
	//      displayOrder = 21
	//  )
	//  public boolean isUseAdaGrad() {
	//    return useAdaGrad;
	//  }
	//
	//  public void setUseAdaGrad(boolean m_useAdaGrad) {
	//    this.useAdaGrad = m_useAdaGrad;
	//  }
	//
	//  @OptionMetadata(
	//      displayName = "batchSize",
	//      description = "The mini-batch size (default = 1000).",
	//      commandLineParamName = "batchSize",
	//      commandLineParamSynopsis = "-batchSize <int>",
	//      displayOrder = 22
	//  )
	//  public int getBatchSize() {
	//    return batchSize;
	//  }
	//
	//  public void setBatchSize(int m_batchSize) {
	//    this.batchSize = m_batchSize;
	//  }
}
