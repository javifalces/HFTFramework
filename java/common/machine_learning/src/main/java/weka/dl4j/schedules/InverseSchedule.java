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
 * InverseSchedule.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.schedules;

import java.util.Enumeration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import weka.core.Option;
import weka.core.OptionMetadata;

/**
 * Inverse exponential schedule for learning rates.
 *
 * @author Steven Lang
 */
@EqualsAndHashCode(callSuper = true) @ToString public class InverseSchedule
		extends Schedule<org.nd4j.linalg.schedule.InverseSchedule> {

	private static final long serialVersionUID = 1840128418738133390L;
	private double gamma = 0.99;
	private double power = 1;

	@OptionMetadata(displayName = "gamma", description = "The gamma value (default = 0.99).", commandLineParamName = "gamma", commandLineParamSynopsis = "-gamma <double>", displayOrder = 2) public double getGamma() {
		return gamma;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	@OptionMetadata(displayName = "power", description = "The power value (default = 1.0).", commandLineParamName = "power", commandLineParamSynopsis = "-power <double>", displayOrder = 3) public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	@Override public org.nd4j.linalg.schedule.InverseSchedule getBackend() {
		return new org.nd4j.linalg.schedule.InverseSchedule(scheduleType.getBackend(), initialValue, gamma, power);
	}

	@Override public void setBackend(org.nd4j.linalg.schedule.InverseSchedule newBackend) {
		this.gamma = newBackend.getGamma();
		this.power = newBackend.getPower();
		this.initialValue = newBackend.getInitialValue();
		this.scheduleType = ScheduleType.fromBackend(newBackend.getScheduleType());
	}

	@Override public void initializeBackend() {
		backend = new org.nd4j.linalg.schedule.InverseSchedule(scheduleType.getBackend(), initialValue, gamma, power);
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
