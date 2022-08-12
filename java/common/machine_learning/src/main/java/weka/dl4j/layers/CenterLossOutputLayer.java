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
 * CenterLossOutputLayer.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import java.io.Serializable;
import java.util.Enumeration;

import org.nd4j.linalg.lossfunctions.ILossFunction;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.lossfunctions.LossFunction;
import weka.dl4j.lossfunctions.LossMCXENT;

/**
 * A version of DeepLearning4j's CenterLossOutputLayer layer that implements WEKA option handling.
 *
 * @author Steven Lang
 */
public class CenterLossOutputLayer extends FeedForwardLayer<org.deeplearning4j.nn.conf.layers.CenterLossOutputLayer>
		implements OptionHandler, Serializable {

	private static final long serialVersionUID = 470708804132984710L;

	/**
	 * Constructor for setting some defaults.
	 */
	public CenterLossOutputLayer() {
		super();
		setLayerName("CenterLossOutput layer");
		setLossFn(new LossMCXENT());
		setGradientCheck(false);
		setAlpha(0.05);
		setLambda(2e-4);
	}

	@OptionMetadata(displayName = "loss function", description = "The loss function to use (default = LossMCXENT).", commandLineParamName = "lossFn", commandLineParamSynopsis = "-lossFn <specification>", displayOrder = 1) public LossFunction<? extends ILossFunction> getLossFn() {
		return LossFunction.create(backend.getLossFn());
	}

	public void setLossFn(LossFunction<? extends ILossFunction> lossFn) {
		backend.setLossFn(lossFn.getBackend());
	}

	@OptionMetadata(displayName = "gradient check", description = "Enable/disable gradient checks (default = false).", commandLineParamName = "gradientCheck", commandLineParamSynopsis = "-gradientCheck <boolean>", displayOrder = 1) public boolean getGradientCheck() {
		return backend.getGradientCheck();
	}

	public void setGradientCheck(boolean gradientCheck) {
		backend.setGradientCheck(gradientCheck);
	}

	@OptionMetadata(displayName = "alpha", description = "The alpha value (default = 0.05).", commandLineParamName = "eps", commandLineParamSynopsis = "-eps <double>", displayOrder = 2) public double getAlpha() {
		return backend.getAlpha();
	}

	public void setAlpha(double alpha) {
		backend.setAlpha(alpha);
	}

	@OptionMetadata(displayName = "lambda", description = "The lambda value (default = 2e-4).", commandLineParamName = "lambda", commandLineParamSynopsis = "-lambda <double>", displayOrder = 2) public double getLambda() {
		return backend.getLambda();
	}

	public void setLambda(double lambda) {
		backend.setLambda(lambda);
	}

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A CenterLossOutput layer from DeepLearning4J.";
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
		this.backend = new org.deeplearning4j.nn.conf.layers.CenterLossOutputLayer();
	}
}
