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
 * ConstantSchedule.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.schedules;

import java.util.Enumeration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.nd4j.linalg.schedule.ISchedule;
import weka.core.Option;
import weka.dl4j.schedules.ConstantSchedule.ConstantScheduleImpl;
import weka.gui.ProgrammaticProperty;

/**
 * Constant schedule for learning rates.
 *
 * @author Steven Lang
 */
@EqualsAndHashCode(callSuper = true) @ToString public class ConstantSchedule extends Schedule<ConstantScheduleImpl> {

	private static final long serialVersionUID = -6950419697195232864L;

	@Override public ConstantScheduleImpl getBackend() {
		return backend;
	}

	@Override public void setBackend(ConstantScheduleImpl newBackend) {
		// Do nothing
	}

	@Override public void initializeBackend() {
		this.backend = new ConstantScheduleImpl(initialValue);
	}

	@Override @ProgrammaticProperty public double getInitialValue() {
		return initialValue;
	}

	@Override @ProgrammaticProperty public void setInitialValue(double initialValue) {
		this.initialValue = initialValue;
		this.backend.setValue(initialValue);
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

	@Data public static class ConstantScheduleImpl implements ISchedule {

		private static final long serialVersionUID = 7134767476736787119L;
		private double value;

		public ConstantScheduleImpl() {
			this(1.0);
		}

		public ConstantScheduleImpl(double value) {
			this.value = value;
		}

		@Override public double valueAt(int iteration, int epoch) {
			return value;
		}

		@Override public ISchedule clone() {
			return new ConstantScheduleImpl(value);
		}
	}
}
