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
 * SeparableConvolution2DLayer.java
 * Copyright (C) 2016-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import weka.core.Option;
import weka.core.OptionHandler;

import java.io.Serializable;
import java.util.Enumeration;

/**
 * A version of DeepLearning4j's SeperableConvolution2DLayer that implements WEKA option handling.
 *
 * @author Rhys Compton
 */
public class SeperableConvolution2DLayer
		extends FeedForwardLayer<org.deeplearning4j.nn.conf.layers.SeparableConvolution2D>
		implements OptionHandler, Serializable {

	/**
	 * The ID used to serialize this class.
	 */
	private static final long serialVersionUID = 6905344091980568487L;

	/**
	 * Constructor for setting some defaults.
	 */
	public SeperableConvolution2DLayer() {
		super();
		setLayerName("Seperable Convolution 2D layer");
	}

	@Override public void initializeBackend() {
		backend = new org.deeplearning4j.nn.conf.layers.SeparableConvolution2D();
	}

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A convolution layer from DeepLearning4J.";
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
