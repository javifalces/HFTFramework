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
 * ZeroPaddingLayer.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import java.io.Serializable;
import java.util.Enumeration;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.gui.ProgrammaticProperty;

/**
 * A version of DeepLearning4j's ZeroPaddingLayer layer that implements WEKA option handling.
 *
 * @author Steven Lang
 */
public class ZeroPaddingLayer extends Layer<org.deeplearning4j.nn.conf.layers.ZeroPaddingLayer>
		implements OptionHandler, Serializable {

	private static final long serialVersionUID = 847105027603786222L;

	/**
	 * Constructor for setting some defaults.
	 */
	public ZeroPaddingLayer() {
		super();
		setPadding(new int[] { 0, 0 });
		setLayerName("ZeroPadding layer");
	}

	@OptionMetadata(displayName = "number of rows in padding", description = "The number of rows in the padding (default = 0).", commandLineParamName = "paddingRows", commandLineParamSynopsis = "-paddingRows <int>", displayOrder = 8) public int getPaddingRows() {
		return backend.getPadding()[0];
	}

	public void setPaddingRows(int padding) {
		int[] pad = new int[] { padding, getPaddingColumns() };
		backend.setPadding(pad);
	}

	@ProgrammaticProperty public int[] getPadding() {
		return backend.getPadding();
	}

	public void setPadding(int[] padding) {
		backend.setPadding(padding);
	}

	@OptionMetadata(displayName = "number of columns in padding", description = "The number of columns in the padding (default = 0).", commandLineParamName = "paddingColumns", commandLineParamSynopsis = "-paddingColumns <int>", displayOrder = 9) public int getPaddingColumns() {
		return backend.getPadding()[1];
	}

	public void setPaddingColumns(int padding) {
		int[] pad = new int[] { getPaddingRows(), padding };
		backend.setPadding(pad);
	}

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A ZeroPadding layer from DeepLearning4J.";
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
		this.backend = new org.deeplearning4j.nn.conf.layers.ZeroPaddingLayer();
	}
}
