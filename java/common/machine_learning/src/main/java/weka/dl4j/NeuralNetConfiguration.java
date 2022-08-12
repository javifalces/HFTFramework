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
 * NeuralNetConfiguration.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j;

import java.io.Serializable;
import java.util.Enumeration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.Builder;
import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.weights.WeightInit;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.distribution.Disabled;
import weka.dl4j.distribution.Distribution;
import weka.dl4j.dropout.AbstractDropout;
import weka.dl4j.enums.GradientNormalization;
import weka.dl4j.updater.Adam;
import weka.dl4j.updater.Sgd;
import weka.dl4j.updater.Updater;
import weka.dl4j.weightnoise.AbstractWeightNoise;
import weka.gui.ProgrammaticProperty;

/**
 * A version of DeepLearning4j's NeuralNetConfiguration that implements WEKA option handling.
 *
 * <p>The duplicate code of configuration parameters is necessary since the dl4j
 * NeuralNetConfiguration.Builder object is not serializable which is necessary for the weka GUI.
 *
 * @author Eibe Frank
 * @author Steven Lang
 */
@EqualsAndHashCode @ToString public class NeuralNetConfiguration implements Serializable, OptionHandler {

	private static final long serialVersionUID = -4384295102884151216L;

	protected WeightInit weightInit = WeightInit.XAVIER;
	protected double biasInit = 0.0;
	protected Distribution dist = new Disabled();
	protected double l1 = Double.NaN;
	protected double l2 = Double.NaN;
	protected AbstractDropout dropout = new weka.dl4j.dropout.Disabled();
	protected Updater updater = new Sgd();
	protected Updater biasUpdater = new Sgd();
	protected boolean miniBatch = true;
	protected long seed = 0;
	protected OptimizationAlgorithm optimizationAlgo = OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT;
	protected boolean useDropConnect = false;
	protected AbstractWeightNoise weightNoise = new weka.dl4j.weightnoise.Disabled();
	protected boolean minimize = true;
	protected GradientNormalization gradientNormalization = GradientNormalization.None;
	protected double gradientNormalizationThreshold = 1.0;
	protected WorkspaceMode inferenceWorkspaceMode = Preferences.WORKSPACE_MODE;
	protected WorkspaceMode trainingWorkspaceMode = Preferences.WORKSPACE_MODE;

	/**
	 * Constructor that provides default values for the settings.
	 */
	public NeuralNetConfiguration() {
		setUpdater(new Adam());
		setWeightInit(WeightInit.XAVIER);
	}

	/**
	 * Deliver access to the internal builder
	 *
	 * @return NeuralNetworkConfiguration
	 */
	public org.deeplearning4j.nn.conf.NeuralNetConfiguration.Builder builder() {
		Builder builder = new Builder();

		// Set dist to null if Disabled was chosen as dl4j backend defaults to null

		builder.l1(l1).l2(l2).optimizationAlgo(optimizationAlgo).seed(seed).biasInit(biasInit)
				.updater(updater.getBackend()).biasUpdater(biasUpdater.getBackend()).dropOut(dropout.getBackend())
				.miniBatch(miniBatch).minimize(minimize).weightNoise(weightNoise.getBackend())
				.gradientNormalization(gradientNormalization.getBackend())
				.gradientNormalizationThreshold(gradientNormalizationThreshold)
				.inferenceWorkspaceMode(inferenceWorkspaceMode).trainingWorkspaceMode(trainingWorkspaceMode);

		if (!(dist instanceof Disabled)) {
			builder.weightInit(dist.getBackend());
		} else {
			builder.weightInit(weightInit);
		}

		return builder;
	}

	@OptionMetadata(description = "Optimization algorithm (LINE_GRADIENT_DESCENT,"
			+ " CONJUGATE_GRADIENT, HESSIAN_FREE, "
			+ "LBFGS, STOCHASTIC_GRADIENT_DESCENT)", displayName = "optimization algorithm", commandLineParamName = "algorithm", commandLineParamSynopsis = "-algorithm <string>", displayOrder = 1) public OptimizationAlgorithm getOptimizationAlgo() {
		return optimizationAlgo;
	}

	public void setOptimizationAlgo(OptimizationAlgorithm optimAlgorithm) {
		optimizationAlgo = optimAlgorithm;
	}

	@OptionMetadata(displayName = "whether to minimize objective", description = "Whether to minimize objective.", commandLineParamIsFlag = true, commandLineParamName = "minimize", commandLineParamSynopsis = "-minimize", displayOrder = 7) public boolean isMinimize() {
		return minimize;
	}

	public void setMinimize(boolean b) {
		minimize = b;
	}

	@OptionMetadata(displayName = "updater", description = "The updater to use (default = SGD).", commandLineParamName = "updater", commandLineParamSynopsis = "-updater <string>", displayOrder = 12) public Updater getUpdater() {
		return updater;
	}

	public void setUpdater(Updater updater) {
		this.updater = updater;
	}

	@OptionMetadata(displayName = "biasUpdater", description = "The updater to use for the bias (default = SGD).", commandLineParamName = "biasUpdater", commandLineParamSynopsis = "-biasUpdater <string>", displayOrder = 13) public Updater getBiasUpdater() {
		return biasUpdater;
	}

	public void setBiasUpdater(Updater biasUpdater) {
		this.biasUpdater = biasUpdater;
	}

	@OptionMetadata(displayName = "dropout", description = "The dropout method to use (default = Dropout(0.0).", commandLineParamName = "dropout", commandLineParamSynopsis = "-dropout <Dropout>", displayOrder = 25) public AbstractDropout getDropout() {
		return dropout;
	}

	public void setDropout(AbstractDropout dropout) {
		this.dropout = dropout;
	}

	@OptionMetadata(displayName = "weightNoise", description = "The weight noise method to use (default = None).", commandLineParamName = "weightNoise", commandLineParamSynopsis = "-weightNoise <WeightNoise>", displayOrder = 26) public AbstractWeightNoise getWeightNoise() {
		return weightNoise;
	}

	public void setWeightNoise(AbstractWeightNoise weightNoise) {
		this.weightNoise = weightNoise;
	}

	@OptionMetadata(displayName = "l1 regularization factor", description = "L1 regularization factor (default = 0.00).", commandLineParamName = "l1", commandLineParamSynopsis = "-l1 <double>", displayOrder = 14) public double getL1() {
		return l1;
	}

	public void setL1(double l1) {
		this.l1 = l1;
	}

	@OptionMetadata(displayName = "l2 regularization factor", description = "L2 regularization factor (default = 0.00).", commandLineParamName = "l2", commandLineParamSynopsis = "-l2 <double>", displayOrder = 15) public double getL2() {
		return l2;
	}

	public void setL2(double l2) {
		this.l2 = l2;
	}

	@OptionMetadata(displayName = "weight initialization method", description = "The method for weight initialization (default = XAVIER).", commandLineParamName = "weightInit", commandLineParamSynopsis = "-weightInit <specification>", displayOrder = 18) public WeightInit getWeightInit() {
		return weightInit;
	}

	public void setWeightInit(WeightInit weightInit) {
		this.weightInit = weightInit;
	}

	@OptionMetadata(displayName = "gradient normalization method", description = "The gradient normalization method (default = None).", commandLineParamName = "gradientNormalization", commandLineParamSynopsis = "-gradientNormalization <specification>", displayOrder = 22) public GradientNormalization getGradientNormalization() {
		return this.gradientNormalization;
	}

	public void setGradientNormalization(GradientNormalization gradientNormalization) {
		this.gradientNormalization = gradientNormalization;
	}

	@OptionMetadata(displayName = "gradient normalization threshold", description = "The gradient normalization threshold (default = 1).", commandLineParamName = "gradNormThreshold", commandLineParamSynopsis = "-gradNormThreshold <double>", displayOrder = 23) public double getGradientNormalizationThreshold() {
		return this.gradientNormalizationThreshold;
	}

	public void setGradientNormalizationThreshold(double gradientNormalizationThreshold) {
		this.gradientNormalizationThreshold = gradientNormalizationThreshold;
	}

	@OptionMetadata(displayName = "distribution", description = "The weight init distribution type. Only applies when weightinit=DISTRIBUTION (default = Disabled).", commandLineParamName = "dist", commandLineParamSynopsis = "-dist <specification>", displayOrder = 19) public Distribution<? extends org.deeplearning4j.nn.conf.distribution.Distribution> getDist() {
		return dist;
	}

	public void setDist(Distribution<? extends org.deeplearning4j.nn.conf.distribution.Distribution> dist) {
		this.dist = dist;
	}

	@OptionMetadata(displayName = "bias initialization", description = "The bias initialization (default = 0.0).", commandLineParamName = "biasInit", commandLineParamSynopsis = "-biasInit <double>", displayOrder = 20) public double getBiasInit() {
		return this.biasInit;
	}

	public void setBiasInit(double biasInit) {
		this.biasInit = biasInit;
	}

	@ProgrammaticProperty public long getSeed() {
		return seed;
	}

	public void setSeed(long n) {
		seed = n;
	}

	@ProgrammaticProperty public boolean isMiniBatch() {
		return miniBatch;
	}

	public void setMiniBatch(boolean b) {
		miniBatch = b;
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration<Option> listOptions() {

		return Option.listOptionsForClass(this.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {

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

	/**
	 * Returns a string describing this search method
	 *
	 * @return a description of the search method suitable for displaying in the explorer/experimenter
	 * gui
	 */
	public String globalInfo() {
		return "Class for fine tuning configurations of the network.\n" + "Parameters set as NaN are unused.\n";
	}
}
