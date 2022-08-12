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
 * FeedForwardLayer.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import java.util.Enumeration;

import weka.core.Option;
import weka.core.OptionMetadata;
import weka.dl4j.activations.Activation;

/**
 * Abstract feed forward layer.
 *
 * @param <T> Feed forward layer implementation
 * @author Steven Lang
 */
public abstract class FeedForwardLayer<T extends org.deeplearning4j.nn.conf.layers.FeedForwardLayer> extends Layer<T> {

	private static final long serialVersionUID = -909125865597886097L;

	public FeedForwardLayer() {
		super();
		backend.setGradientNormalizationThreshold(Double.NaN);
		backend.setGradientNormalization(null);
		backend.setBiasInit(Double.NaN);
		setActivationFunction(new weka.dl4j.activations.ActivationIdentity());
	}

	@OptionMetadata(displayName = "activation function", description = "The activation function to use (default = Identity).", commandLineParamName = "activation", commandLineParamSynopsis = "-activation <specification>", displayOrder = 1) public Activation getActivationFunction() {
		return Activation.create(backend.getActivationFn());
	}

	public void setActivationFunction(Activation activationFn) {
		backend.setActivationFn(activationFn.getBackend());
	}

	@OptionMetadata(displayName = "number of outputs", description = "The number of outputs.", commandLineParamName = "nOut", commandLineParamSynopsis = "-nOut <int>", displayOrder = 2) public long getNOut() {
		return backend.getNOut();
	}

	public void setNOut(long nOut) {
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
}
