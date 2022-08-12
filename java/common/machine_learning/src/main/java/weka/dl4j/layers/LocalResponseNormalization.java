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
 * LocalResponseNormalization.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import java.util.Enumeration;

import weka.core.Option;

/**
 * A version of DeepLearning4j's LocalResponseNormalization layer that implements WEKA option
 * handling.
 *
 * @author Steven Lang
 */
public class LocalResponseNormalization extends Layer<org.deeplearning4j.nn.conf.layers.LocalResponseNormalization> {

	private static final long serialVersionUID = 1846770958541767815L;

	/**
	 * Constructor for setting some defaults.
	 */
	public LocalResponseNormalization() {
		super();
		setLayerName("LocalResponseNormalization layer");
	}

	@Override public void initializeBackend() {
		backend = new org.deeplearning4j.nn.conf.layers.LocalResponseNormalization();
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

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A local response normalization layer from DeepLearning4J.";
	}

}
