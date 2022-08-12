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
 * BatchNormalization.java
 * Copyright (C) 2016-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import java.io.Serializable;
import java.util.Enumeration;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.activations.ActivationIdentity;
import weka.gui.ProgrammaticProperty;

/**
 * A version of DeepLearning4j's BatchNormalization layer that implements WEKA option handling.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 * @author Steven Lang
 */
public class BatchNormalization extends FeedForwardLayer<org.deeplearning4j.nn.conf.layers.BatchNormalization>
		implements OptionHandler, Serializable {

	/**
	 * The ID used to serialize this class.
	 */
	private static final long serialVersionUID = 6804344091980568487L;

	/**
	 * Constructor for setting some defaults.
	 */
	public BatchNormalization() {
		super();
		setLayerName("Batch normalization layer");
		setActivationFunction(new ActivationIdentity());
	}

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A batch norm layer from DeepLearning4J.";
	}

	@OptionMetadata(displayName = "decay parameter", description = "The decay parameter (default = 0.9).", commandLineParamName = "decay", commandLineParamSynopsis = "-decay <double>", displayOrder = 1) public double getDecay() {
		return backend.getDecay();
	}

	public void setDecay(double decay) {
		backend.setDecay(decay);
	}

	@OptionMetadata(displayName = "eps parameter", description = "The eps parameter (default = 1e-5).", commandLineParamName = "eps", commandLineParamSynopsis = "-eps <double>", displayOrder = 2) public double getEps() {
		return backend.getEps();
	}

	public void setEps(double eps) {
		backend.setEps(eps);
	}

	@OptionMetadata(displayName = "gamma parameter", description = "The gamma parameter (default = 1).", commandLineParamName = "gamma", commandLineParamSynopsis = "-gamma <double>", displayOrder = 3) public double getGamma() {
		return backend.getGamma();
	}

	public void setGamma(double gamma) {
		backend.setGamma(gamma);
	}

	@OptionMetadata(displayName = "beta parameter", description = "The beta parameter (default = 0).", commandLineParamName = "beta", commandLineParamSynopsis = "-beta <double>", displayOrder = 4) public double getBeta() {
		return backend.getBeta();
	}

	public void setBeta(double beta) {
		backend.setBeta(beta);
	}

	@OptionMetadata(displayName = "lock gamma and beta", description = "Whether to lock gamma and beta.", commandLineParamName = "beta", commandLineParamSynopsis = "-lockGammaBeta", displayOrder = 5) public boolean getLockGammaAndBeta() {
		return backend.isLockGammaBeta();
	}

	public void setLockGammaAndBeta(boolean lgb) {
		backend.setLockGammaBeta(lgb);
	}

	@ProgrammaticProperty public boolean isLockGammaBeta() {
		return backend.isLockGammaBeta();
	}

	public void setLockGammaBeta(boolean lgb) {
		backend.setLockGammaBeta(lgb);
	}

	@OptionMetadata(displayName = "isMinibatch", description = "Whether minibatches are not not used.", commandLineParamName = "isMinibatch", commandLineParamSynopsis = "-isMinibatch", displayOrder = 6) public boolean getIsMinibatch() {
		return backend.isMinibatch();
	}

	public void setMinibatch(boolean b) {
		backend.setMinibatch(b);
	}

	@ProgrammaticProperty @Deprecated public long getNOut() {
		return backend.getNOut();
	}

	@ProgrammaticProperty @Deprecated public void setNOut(long nOut) {
		backend.setNOut(nOut);
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override public Enumeration<Option> listOptions() {
		return Option.listOptionsForClassHierarchy(this.getClass(), super.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override public String[] getOptions() {
		return Option.getOptionsForHierarchy(this, super.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		Option.setOptionsForHierarchy(options, this, super.getClass());
	}

	@Override public void initializeBackend() {
		this.backend = new org.deeplearning4j.nn.conf.layers.BatchNormalization();
	}
}
