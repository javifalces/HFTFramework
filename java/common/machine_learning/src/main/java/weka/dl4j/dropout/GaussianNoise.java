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
 * GaussianNoise.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.dropout;

import java.util.Enumeration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.nd4j.linalg.schedule.ISchedule;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.dl4j.schedules.ConstantSchedule;
import weka.dl4j.schedules.Schedule;

/**
 * Gaussian noise implementation.
 *
 * @author Steven Lang
 */
@EqualsAndHashCode(callSuper = true) @ToString public class GaussianNoise
		extends AbstractDropout<org.deeplearning4j.nn.conf.dropout.GaussianNoise> {

	private static final long serialVersionUID = -294245467732026881L;
	private Schedule<? extends ISchedule> rateSchedule;
	private double stdDev;

	public double getStdDev() {
		return stdDev;
	}

	@OptionMetadata(displayName = "schedule", description = "The standard deviation (default = 1.0).", commandLineParamName = "schedule", commandLineParamSynopsis = "-schedule <Schedule>", displayOrder = 2) public void setStdDev(
			double stdDev) {
		this.stdDev = stdDev;
	}

	@OptionMetadata(displayName = "schedule", description = "The standard deviation schedule (default = ConstantScheduleImpl).", commandLineParamName = "schedule", commandLineParamSynopsis = "-schedule <Schedule>", displayOrder = 2) public Schedule<? extends ISchedule> getRateSchedule() {
		return rateSchedule;
	}

	public void setRateSchedule(Schedule<? extends ISchedule> rateSchedule) {
		this.rateSchedule = rateSchedule;
	}

	@Override public org.deeplearning4j.nn.conf.dropout.GaussianNoise getBackend() {
		return new org.deeplearning4j.nn.conf.dropout.GaussianNoise(rateSchedule.getBackend());
	}

	@Override public void initializeBackend() {
		rateSchedule = new ConstantSchedule();
		backend = new org.deeplearning4j.nn.conf.dropout.GaussianNoise(rateSchedule.getBackend());
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
