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
 * DenseLayer.java
 * Copyright (C) 2016-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.dl4j.activations.ActivationReLU;

import java.io.Serializable;
import java.util.Enumeration;

/**
 * A version of DeepLearning4j's DenseLayer that implements WEKA option handling.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 * @author Steven Lang
 */
public class Cropping2D extends NoParamLayer<org.deeplearning4j.nn.conf.layers.convolutional.Cropping2D>
		implements OptionHandler, Serializable {

	// The serial version ID used when serializing this class
	protected static final long serialVersionUID = -6905811990400L;

	/**
	 * Constructor for setting some defaults.
	 */
	public Cropping2D() {
		super();
		setLayerName("Cropping2D");
		//    setActivationFunction(new ActivationReLU());
	}

	@Override public void initializeBackend() {
		backend = new org.deeplearning4j.nn.conf.layers.convolutional.Cropping2D();
	}

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A cropping 2D layer.";
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
