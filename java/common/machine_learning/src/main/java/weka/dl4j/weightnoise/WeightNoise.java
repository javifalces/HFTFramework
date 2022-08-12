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
 * WeightNoise.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.weightnoise;

import java.io.Serializable;
import java.util.Enumeration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.distribution.Distribution;

/**
 * Weight noise wrapper.
 *
 * @author Steven Lang
 */
@EqualsAndHashCode(callSuper = true) @ToString public class WeightNoise
		extends AbstractWeightNoise<org.deeplearning4j.nn.conf.weightnoise.WeightNoise>
		implements OptionHandler, Serializable {

	private static final long serialVersionUID = 7880719223147030268L;

	@OptionMetadata(displayName = "distribution", description = "The weight noise distribution (default = NormalDistribution(0,1)).", commandLineParamName = "distribution", commandLineParamSynopsis = "-distribution <Distribution>", displayOrder = 1) public Distribution<? extends org.deeplearning4j.nn.conf.distribution.Distribution> getDistribution() {
		return Distribution.create(backend.getDistribution());
	}

	public void setDistribution(
			Distribution<? extends org.deeplearning4j.nn.conf.distribution.Distribution> distribution) {
		backend.setDistribution(distribution.getBackend());
	}

	@OptionMetadata(displayName = "applyToBias", description = "Whether to apply it to the bias as well (default = false).", commandLineParamName = "applyToBias", commandLineParamSynopsis = "-applyToBias <boolean>", displayOrder = 2) public boolean isApplyToBias() {
		return backend.isApplyToBias();
	}

	public void setApplyToBias(boolean applyToBias) {
		backend.setApplyToBias(applyToBias);
	}

	@OptionMetadata(displayName = "isAdditive", description = "Whether noise is added to weights or multiplied (default = true).", commandLineParamName = "isAdditive", commandLineParamSynopsis = "-isAdditive <boolean>", displayOrder = 2) public boolean isAdditive() {
		return backend.isAdditive();
	}

	public void setAdditive(boolean additive) {
		backend.setAdditive(additive);
	}

	@Override public void initializeBackend() {
		backend = new org.deeplearning4j.nn.conf.weightnoise.WeightNoise(new NormalDistribution(0, 1));
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
