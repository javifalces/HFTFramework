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
 * Dropout.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.dropout;

import java.util.Enumeration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.nd4j.linalg.schedule.ISchedule;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.dl4j.schedules.ConstantSchedule.ConstantScheduleImpl;
import weka.dl4j.schedules.Schedule;

@EqualsAndHashCode(callSuper = true) @ToString public class Dropout
		extends AbstractDropout<org.deeplearning4j.nn.conf.dropout.Dropout> {

	private static final long serialVersionUID = 8473917398934823240L;

	/**
	 * Get the learning rate schedule
	 *
	 * @return Learning rate schedule
	 */
	@OptionMetadata(displayName = "p", description = "The dropout probability (default = 0.5).", commandLineParamName = "p", commandLineParamSynopsis = "-p <double>", displayOrder = 1) public double getP() {
		return backend.getP();
	}

	public void setP(double p) {
		backend.setP(p);
	}

	@OptionMetadata(displayName = "pSchedule", description = "The dropout probability schedule  (default = ConstantScheduleImpl).", commandLineParamName = "pSchedule", commandLineParamSynopsis = "-pSchedule <Schedule>", displayOrder = 2) public Schedule<? extends ISchedule> getpSchedule() {
		return Schedule.create(backend.getPSchedule());
	}

	public void setpSchedule(Schedule<? extends ISchedule> pSchedule) {
		backend.setPSchedule(pSchedule.getBackend());
	}

	@Override public void initializeBackend() {
		backend = new org.deeplearning4j.nn.conf.dropout.Dropout(0.5);
		backend.setPSchedule(new ConstantScheduleImpl());
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
