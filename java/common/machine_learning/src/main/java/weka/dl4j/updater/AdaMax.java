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
 * AdaMax.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.updater;

import java.util.Enumeration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import weka.core.Option;
import weka.core.OptionMetadata;

/**
 * A WEKA version of DeepLearning4j's AdaMax.
 *
 * @author Steven Lang
 */
@EqualsAndHashCode(callSuper = true) @ToString public class AdaMax
		extends Updater<org.nd4j.linalg.learning.config.AdaMax> {

	private static final long serialVersionUID = 9196053591015785889L;

	@OptionMetadata(displayName = "beta1MeanDecay", description = "The mean decay (default = "
			+ org.nd4j.linalg.learning.config.AdaMax.DEFAULT_ADAMAX_BETA1_MEAN_DECAY
			+ ").", commandLineParamName = "beta1MeanDecay", commandLineParamSynopsis = "-beta1MeanDecay <double>", displayOrder = 1) public double getBeta1() {
		return backend.getBeta1();
	}

	public void setBeta1(double beta1) {
		backend.setBeta1(beta1);
	}

	@OptionMetadata(displayName = "beta2VarDecay", description = "The var decay (default = "
			+ org.nd4j.linalg.learning.config.AdaMax.DEFAULT_ADAMAX_BETA2_VAR_DECAY
			+ ").", commandLineParamName = "beta2VarDecay", commandLineParamSynopsis = "-beta2VarDecay <double>", displayOrder = 2) public double getBeta2() {
		return backend.getBeta2();
	}

	public void setBeta2(double beta2) {
		backend.setBeta2(beta2);
	}

	@OptionMetadata(displayName = "epsilon", description = "The epsilon parameter (default = "
			+ org.nd4j.linalg.learning.config.AdaMax.DEFAULT_ADAMAX_EPSILON
			+ ").", commandLineParamName = "epsilon", commandLineParamSynopsis = "-epsilon <double>", displayOrder = 3) public double getEpsilon() {
		return backend.getEpsilon();
	}

	public void setEpsilon(double epsilon) {
		backend.setEpsilon(epsilon);
	}

	@Override public void initializeBackend() {
		backend = new org.nd4j.linalg.learning.config.AdaMax();
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
