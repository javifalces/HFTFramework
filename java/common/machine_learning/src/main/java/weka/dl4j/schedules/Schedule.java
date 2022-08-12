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
 * Schedule.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.schedules;

import java.io.Serializable;
import java.util.Enumeration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.nd4j.linalg.schedule.ISchedule;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.ApiWrapper;
import weka.dl4j.ApiWrapperUtil;
import weka.gui.ProgrammaticProperty;

/**
 * Default schedule interface that implements WEKA option handling. Each class that implements
 * Schedulers extending from this class must have a backing implementation of the referencing Dl4j
 * scheduler since the scheduler in Dl4j do not allow access to their class members (private
 * final).
 * <p>
 * SigmoidSchedule, StepSchedule)
 *
 * @author Steven Lang
 */
@EqualsAndHashCode @ToString public abstract class Schedule<T extends ISchedule>
		implements OptionHandler, ApiWrapper<T>, Serializable {

	private static final long serialVersionUID = 5588471135175058051L;

	/**
	 * Schedule type
	 */
	ScheduleType scheduleType = ScheduleType.EPOCH;

	/**
	 * Initial value
	 */
	double initialValue = 1.0;

	/**
	 * Schedule that is backing the implementation
	 */
	T backend;

	public Schedule() {
		initializeBackend();
	}

	/**
	 * Create an API wrapped schedule from a given ISchedule object.
	 *
	 * @param newBackend Backend object
	 * @return API wrapped object
	 */
	public static Schedule<? extends ISchedule> create(ISchedule newBackend) {
		if (newBackend == null) {
			return new ConstantSchedule();
		}
		return ApiWrapperUtil.getImplementingWrapper(Schedule.class, newBackend, "weka.dl4j.schedules");
	}

	@OptionMetadata(displayName = "scheduleType", description = "The schedule type, one of {EPOCH,ITERATION} (default = EPOCH).", commandLineParamName = "scheduleType", commandLineParamSynopsis = "-scheduleType <string>", displayOrder = 1) public ScheduleType getScheduleType() {
		return scheduleType;
	}

	public void setScheduleType(ScheduleType scheduleType) {
		this.scheduleType = scheduleType;
	}

	@ProgrammaticProperty public double getInitialValue() {
		return initialValue;
	}

	@ProgrammaticProperty public void setInitialValue(double initialValue) {
		this.initialValue = initialValue;
	}

	@Override public T getBackend() {
		initializeBackend();
		return backend;
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
}
