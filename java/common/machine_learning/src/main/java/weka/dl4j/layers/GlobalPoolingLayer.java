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
 * GlobalPoolingLayer.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import java.util.Enumeration;

import weka.core.Option;
import weka.core.OptionMetadata;
import weka.dl4j.enums.PoolingType;

/**
 * A version of DeepLearning4j's GlobalPooling that implements WEKA option handling.
 *
 * @author Steven Lang
 */
public class GlobalPoolingLayer extends Layer<org.deeplearning4j.nn.conf.layers.GlobalPoolingLayer> {

	private static final long serialVersionUID = 2882286002911860559L;

	/**
	 * Constructor for setting some defaults.
	 */
	public GlobalPoolingLayer() {
		super();
		setLayerName("GlobalPooling layer");
		setPoolingType(PoolingType.MAX);
		setPnorm(2);
	}

	@Override public void initializeBackend() {
		backend = new org.deeplearning4j.nn.conf.layers.GlobalPoolingLayer();
	}

	@OptionMetadata(displayName = "pooling type", description = "The type of pooling to use (default = MAX; options: MAX, AVG, SUM, NONE).", commandLineParamName = "poolingType", commandLineParamSynopsis = "-poolingType <string>", displayOrder = 10) public PoolingType getPoolingType() {
		return PoolingType.fromBackend(backend.getPoolingType());
	}

	public void setPoolingType(PoolingType poolingType) {
		backend.setPoolingType(poolingType.getBackend());
	}

	@OptionMetadata(displayName = "pooling dimensions", description = "The pooling dimensions (default = [2,2]).", commandLineParamName = "poolDimensions", commandLineParamSynopsis = "-poolDimensions <int>", displayOrder = 4) public int[] getPoolingDimensions() {
		return backend.getPoolingDimensions();
	}

	public void setPoolingDimensions(int[] poolingDimensions) {
		backend.setPoolingDimensions(poolingDimensions);
	}

	@OptionMetadata(displayName = "pnorm", description = "The value of the pnorm parameter (default = 2).", commandLineParamName = "pnorm", commandLineParamSynopsis = "-pnorm <int>", displayOrder = 3) public int getPnorm() {
		return backend.getPnorm();
	}

	public void setPnorm(int pnorm) {
		backend.setPnorm(pnorm);
	}

	@OptionMetadata(displayName = "collapse dimensions", description = "Wether to collapse dimensions (default = true).", commandLineParamName = "collapseDimensions", commandLineParamSynopsis = "-collapseDimensions <boolean>", displayOrder = 11) public boolean isCollapseDimensions() {
		return backend.isCollapseDimensions();
	}

	public void setCollapseDimensions(boolean collapseDimensions) {
		backend.setCollapseDimensions(collapseDimensions);
	}

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A global pooling layer from DeepLearning4J.";
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
