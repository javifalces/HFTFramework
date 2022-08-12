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
 * ScheduleType.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.schedules;

import weka.dl4j.ApiWrapper;

/**
 * Proxy Enum for {@link org.nd4j.linalg.schedule.ScheduleType}. This is necessary as Weka's run
 * script cannot find the enum classes during the option parsing as they reside in the Dl4j backend
 * and are at that time not visible to the class loader.
 *
 * @author Steven Lang
 */
public enum ScheduleType implements ApiWrapper<org.nd4j.linalg.schedule.ScheduleType> {
	ITERATION, EPOCH;

	/**
	 * Parse backend schedule type and return weka enum implementation.
	 *
	 * @param scheduleType Schedule type
	 * @return Weka schedule type implementation
	 */
	public static ScheduleType fromBackend(org.nd4j.linalg.schedule.ScheduleType scheduleType) {
		return valueOf(scheduleType.name());
	}

	public org.nd4j.linalg.schedule.ScheduleType getBackend() {
		switch (this) {
			case EPOCH:
				return org.nd4j.linalg.schedule.ScheduleType.EPOCH;
			case ITERATION:
				return org.nd4j.linalg.schedule.ScheduleType.ITERATION;
			default:
				throw new RuntimeException(
						"GradientNormalization method: " + this + " not found in Deeplearning4j backend.");
		}
	}

	@Override public void setBackend(org.nd4j.linalg.schedule.ScheduleType newBackend) {
		// Do nothing as this enum does not have a state
	}

	@Override public void initializeBackend() {
		// Do nothing as this enum does not have a state
	}
}
