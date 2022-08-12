//package weka.dl4j.layers;
//
//import java.io.Serializable;
//import java.util.Enumeration;
//import java.util.Map;
//import org.deeplearning4j.nn.conf.GradientNormalization;
//import org.deeplearning4j.nn.conf.Updater;
//import org.deeplearning4j.nn.conf.distribution.Distribution;
//import org.deeplearning4j.nn.weights.WeightInit;
//import org.nd4j.linalg.activations.IActivation;
//import weka.core.Option;
//import weka.core.OptionHandler;
//import weka.core.OptionMetadata;
//import weka.dl4j.activations.ActivationReLU;
//import weka.dl4j.activations.ActivationSigmoid;
//import weka.gui.ProgrammaticProperty;
//
///**
// * A version of DeepLearning4j's EmbeddingLayer layer that implements WEKA option handling.
// *
// * @author Steven Lang
// */
//public class EmbeddingLayer extends org.deeplearning4j.nn.conf.layers.EmbeddingLayer
//    implements OptionHandler, Serializable {
//  /** SerialVersionUID */
//  private static final long serialVersionUID = 7681606601452628181L;
//
//  /** Constructor for setting some defaults. */
//  public EmbeddingLayer() {
//    setLayerName("Embedding layer");
//    setActivationFunction(new ActivationReLU());
//    setLearningRate(Double.NaN);
//    setBiasLearningRate(Double.NaN);
//    setMomentum(Double.NaN);
//    setBiasInit(Double.NaN);
//    setAdamMeanDecay(Double.NaN);
//    setAdamVarDecay(Double.NaN);
//    setEpsilon(Double.NaN);
//    setRmsDecay(Double.NaN);
//    setL1(Double.NaN);
//    setL2(Double.NaN);
//    setRho(Double.NaN);
//    setGradientNormalization(null);
//    setGradientNormalizationThreshold(Double.NaN);
//  }
//
//  /**
//   * Global info.
//   *
//   * @return string describing this class.
//   */
//  public String globalInfo() {
//    return "A densely connected layer from DeepLearning4J.";
//  }
//
//  @OptionMetadata(
//    displayName = "layer name",
//    description = "The name of the layer (default = Hidden Layer).",
//    commandLineParamName = "name",
//    commandLineParamSynopsis = "-name <string>",
//    displayOrder = 0
//  )
//  public String getLayerName() {
//    return this.layerName;
//  }
//
//  public void setLayerName(String layerName) {
//    this.layerName = layerName;
//  }
//
//  @OptionMetadata(
//    displayName = "number of units",
//    description = "The number of units.",
//    commandLineParamName = "nOut",
//    commandLineParamSynopsis = "-nOut <int>",
//    displayOrder = 1
//  )
//  public int getNOut() {
//    return super.getNOut();
//  }
//
//  public void setNOut(int nOut) {
//    this.nOut = nOut;
//  }
//
//  @OptionMetadata(
//    displayName = "activation function",
//    description = "The activation function to use (default = ActivationSoftmax).",
//    commandLineParamName = "activation",
//    commandLineParamSynopsis = "-activation <specification>",
//    displayOrder = 2
//  )
//  public IActivation getActivationFunction() {
//    return this.activationFn;
//  }
//
//  public void setActivationFunction(IActivation activationFn) {
//    this.activationFn = activationFn;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public IActivation getActivationFn() {
//    return super.getActivationFn();
//  }
//
//  public void setActivationFn(IActivation fn) {
//    super.setActivationFn(fn);
//  }
//
//  @OptionMetadata(
//    displayName = "dropout parameter",
//    description = "The dropout parameter (default = 0).",
//    commandLineParamName = "dropout",
//    commandLineParamSynopsis = "-dropout <double>",
//    displayOrder = 15
//  )
//  public double getDropOut() {
//    return this.dropout;
//  }
//
//  public void setDropOut(double dropout) {
//    this.dropout = dropout;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public WeightInit getWeightInit() {
//    return this.weightInit;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setWeightInit(WeightInit weightInit) {
//    this.weightInit = weightInit;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getBiasInit() {
//    return this.biasInit;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setBiasInit(double biasInit) {
//    this.biasInit = biasInit;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public Distribution getDist() {
//    return this.dist;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setDist(Distribution dist) {
//    this.dist = dist;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getLearningRate() {
//    return this.learningRate;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setLearningRate(double learningRate) {
//    this.learningRate = learningRate;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getBiasLearningRate() {
//    return this.biasLearningRate;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setBiasLearningRate(double biasLearningRate) {
//    this.biasLearningRate = biasLearningRate;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public Map<Integer, Double> getLearningRateSchedule() {
//    return this.learningRateSchedule;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setLearningRateSchedule(Map<Integer, Double> learningRateSchedule) {
//    this.learningRateSchedule = learningRateSchedule;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getMomentum() {
//    return this.momentum;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setMomentum(double momentum) {
//    this.momentum = momentum;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public Map<Integer, Double> getMomentumSchedule() {
//    return this.momentumSchedule;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setMomentumSchedule(Map<Integer, Double> momentumSchedule) {
//    this.momentumSchedule = momentumSchedule;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getL1() {
//    return this.l1;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setL1(double l1) {
//    this.l1 = l1;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getL2() {
//    return this.l2;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setL2(double l2) {
//    this.l2 = l2;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getBiasL1() {
//    return this.l1Bias;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setBiasL1(double biasL1) {
//    this.l1Bias = biasL1;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getBiasL2() {
//    return this.l2Bias;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setBiasL2(double biasL2) {
//    this.l2Bias = biasL2;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public Updater getUpdater() {
//    return this.updater;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setUpdater(Updater updater) {
//    this.updater = updater;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getRho() {
//    return this.rho;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setRho(double rho) {
//    this.rho = rho;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getEpsilon() {
//    return this.epsilon;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setEpsilon(double epsilon) {
//    this.epsilon = epsilon;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getRmsDecay() {
//    return this.rmsDecay;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setRmsDecay(double rmsDecay) {
//    this.rmsDecay = rmsDecay;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getAdamMeanDecay() {
//    return this.adamMeanDecay;
//  }
//
//  public void setAdamMeanDecay(double adamMeanDecay) {
//    this.adamMeanDecay = adamMeanDecay;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getAdamVarDecay() {
//    return this.adamVarDecay;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setAdamVarDecay(double adamVarDecay) {
//    this.adamVarDecay = adamVarDecay;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public GradientNormalization getGradientNormalization() {
//    return this.gradientNormalization;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setGradientNormalization(GradientNormalization gradientNormalization) {
//    this.gradientNormalization = gradientNormalization;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getGradientNormalizationThreshold() {
//    return this.gradientNormalizationThreshold;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public void setGradientNormalizationThreshold(double gradientNormalizationThreshold) {
//    this.gradientNormalizationThreshold = gradientNormalizationThreshold;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public int getNIn() {
//    return super.getNIn();
//  }
//
//  public void setNIn(int nIn) {
//    this.nIn = nIn;
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getL1Bias() {
//    return super.getL1Bias();
//  }
//
//  public void setL1Bias(int l1bias) {
//    super.setL1Bias(l1bias);
//  }
//
//  @ProgrammaticProperty
//  @Deprecated
//  public double getL2Bias() {
//    return super.getL2Bias();
//  }
//
//  public void setL2Bias(int l2bias) {
//    super.setL2Bias(l2bias);
//  }
//
//  /**
//   * Returns an enumeration describing the available options.
//   *
//   * @return an enumeration of all the available options.
//   */
//  @Override
//  public Enumeration<Option> listOptions() {
//
//    return Option.listOptionsForClass(this.getClass()).elements();
//  }
//
//  /**
//   * Gets the current settings of the Classifier.
//   *
//   * @return an array of strings suitable for passing to setOptions
//   */
//  @Override
//  public String[] getOptions() {
//
//    return Option.getOptions(this, this.getClass());
//  }
//
//  /**
//   * Parses a given list of options.
//   *
//   * @param options the list of options as an array of strings
//   * @throws Exception if an option is not supported
//   */
//  public void setOptions(String[] options) throws Exception {
//
//    Option.setOptions(options, this, this.getClass());
//  }
//}
