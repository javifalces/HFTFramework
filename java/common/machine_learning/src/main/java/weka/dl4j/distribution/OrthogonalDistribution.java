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
 * OrthogonalDistribution.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.distribution;

import java.util.Enumeration;

import org.nd4j.shade.jackson.annotation.JsonTypeName;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;

/**
 * A version of DeepLearning4j's OrthogonalDistribution that implements WEKA option handling.
 *
 * @author Eibe Frank
 * @author Steven Lang
 */
@JsonTypeName("orthogonal") public class OrthogonalDistribution
		extends Distribution<org.deeplearning4j.nn.conf.distribution.OrthogonalDistribution> implements OptionHandler {

	private static final long serialVersionUID = 1244534661808099971L;

	@OptionMetadata(displayName = "gain", description = "The distribution gain (default = 1.0).", commandLineParamName = "gain", commandLineParamSynopsis = "-gain <double>", displayOrder = 1) public double getGain() {
		return backend.getGain();
	}

	public void setGain(double gain) {
		backend.setGain(gain);
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override public Enumeration<Option> listOptions() {

		return Option.listOptionsForClass(this.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override public String[] getOptions() {

		return Option.getOptions(this, this.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {

		Option.setOptions(options, this, this.getClass());
	}

	@Override public void initializeBackend() {
		// Constructions normal distribution with mean 0 and unit variance
		backend = new org.deeplearning4j.nn.conf.distribution.OrthogonalDistribution(1.0);
	}
}
