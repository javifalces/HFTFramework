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
 * ConvolutionLayer.java
 * Copyright (C) 2016-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import org.deeplearning4j.nn.conf.layers.DepthwiseConvolution2D;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.enums.AlgoMode;
import weka.dl4j.enums.ConvolutionMode;
import weka.dl4j.activations.ActivationIdentity;

import java.io.Serializable;
import java.util.Enumeration;

/**
 * A version of DeepLearning4j's DepthwiseConvolution2DLayer that implements WEKA option handling.
 *
 * @author Rhys Compton
 */
public class DepthwiseConvolution2DLayer extends FeedForwardLayer<DepthwiseConvolution2D>
		implements OptionHandler, Serializable {

	/**
	 * The ID used to serialize this class.
	 */
	private static final long serialVersionUID = 1252151635L;

	/**
	 * Constructor for setting some defaults.
	 */
	public DepthwiseConvolution2DLayer() {
		super();
		setLayerName("Depthwise Convolution 2D layer");
		setActivationFunction(new ActivationIdentity());
		setConvolutionMode(ConvolutionMode.Truncate);
		setCudnnAlgoMode(AlgoMode.PREFER_FASTEST);
	}

	@Override public void initializeBackend() {
		backend = new DepthwiseConvolution2D();
	}

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A Depthwise Convolution 2D convolution layer from DeepLearning4J.";
	}

	@OptionMetadata(displayName = "convolution mode", description = "The convolution mode (default = Truncate).", commandLineParamName = "mode", commandLineParamSynopsis = "-mode <string>", displayOrder = 2) public ConvolutionMode getConvolutionMode() {
		return ConvolutionMode.fromBackend(backend.getConvolutionMode());
	}

	public void setConvolutionMode(ConvolutionMode convolutionMode) {
		backend.setConvolutionMode(convolutionMode.getBackend());
	}

	@OptionMetadata(displayName = "CudnnAlgoMode", description = "The Cudnn algo mode (default = PREFER_FASTEST).", commandLineParamName = "cudnnAlgoMode", commandLineParamSynopsis = "-cudnnAlgoMode <string>", displayOrder = 3) public AlgoMode getCudnnAlgoMode() {
		return AlgoMode.fromBackend(backend.getCudnnAlgoMode());
	}

	public void setCudnnAlgoMode(AlgoMode cudnnAlgoMode) {
		backend.setCudnnAlgoMode(cudnnAlgoMode.getBackend());
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
