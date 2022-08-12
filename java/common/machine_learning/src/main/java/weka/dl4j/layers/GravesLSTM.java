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
 * GravesLSTM.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import java.io.Serializable;
import java.util.Enumeration;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.activations.Activation;
import weka.dl4j.activations.ActivationReLU;
import weka.dl4j.activations.ActivationSigmoid;

/**
 * A version of DeepLearning4j's GravesLSTM layer that implements WEKA option handling.
 *
 * @author Steven Lang
 */
@Deprecated public class GravesLSTM extends FeedForwardLayer<org.deeplearning4j.nn.conf.layers.GravesLSTM>
		implements OptionHandler, Serializable {

	/**
	 * SerialVersionUID
	 */
	private static final long serialVersionUID = -8982185320769591891L;

	/**
	 * Constructor for setting some defaults.
	 */
	public GravesLSTM() {
		super();
		setLayerName("GravesLSTM layer");
		setActivationFunction(new ActivationReLU());
		setGateActivationFn(new ActivationSigmoid());
	}

	@Override public void initializeBackend() {
		backend = new org.deeplearning4j.nn.conf.layers.GravesLSTM();
	}

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A Graves LSTM layer from DeepLearning4J.";
	}

	@OptionMetadata(displayName = "gate activation function", description = "The activation function to use for the gates (default = ActivationSigmoid).", commandLineParamName = "gateActivation", commandLineParamSynopsis = "-gateActivation <specification>", displayOrder = 2) public Activation getGateActivationFn() {
		return Activation.create(backend.getGateActivationFn());
	}

	public void setGateActivationFn(Activation gateActivationFn) {
		backend.setGateActivationFn(gateActivationFn.getBackend());
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
}
