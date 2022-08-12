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
 * AlphaDropout.java
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
 * Gaussian dropout implementation.
 *
 * @author Steven Lang
 */
@EqualsAndHashCode(callSuper = true) @ToString public class AlphaDropout
		extends AbstractDropout<org.deeplearning4j.nn.conf.dropout.AlphaDropout> {

	private static final long serialVersionUID = -294245467732026881L;

	protected double p;
	protected Schedule<? extends ISchedule> pSchedule;
	protected double alpha;
	protected double lambda;

	@OptionMetadata(displayName = "schedule", description = "The dropout probability schedule (default = ConstantScheduleImpl).", commandLineParamName = "schedule", commandLineParamSynopsis = "-schedule <Schedule>", displayOrder = 1) public Schedule<? extends ISchedule> getpSchedule() {
		return pSchedule;
	}

	public void setpSchedule(Schedule<? extends ISchedule> pSchedule) {
		this.pSchedule = pSchedule;
	}

	@OptionMetadata(displayName = "p", description = "The dropout probability (default = 0.5).", commandLineParamName = "p", commandLineParamSynopsis = "-p <double>", displayOrder = 2) public double getP() {
		return p;
	}

	public void setP(double p) {
		this.p = p;
	}

	@OptionMetadata(displayName = "alpha", description =
			"The alpha value that weight randomly are assigned to (default = "
					+ org.deeplearning4j.nn.conf.dropout.AlphaDropout.DEFAULT_ALPHA
					+ ").", commandLineParamName = "alpha", commandLineParamSynopsis = "-alpha <double>", displayOrder = 3) public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	@OptionMetadata(displayName = "lambda", description = "The lambda value (default = "
			+ org.deeplearning4j.nn.conf.dropout.AlphaDropout.DEFAULT_LAMBDA
			+ ").", commandLineParamName = "lambda", commandLineParamSynopsis = "-lambda <double>", displayOrder = 4) public double getLambda() {
		return lambda;
	}

	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

	@Override public org.deeplearning4j.nn.conf.dropout.AlphaDropout getBackend() {
		if (pSchedule instanceof ConstantSchedule) {
			return new org.deeplearning4j.nn.conf.dropout.AlphaDropout(p);
		} else {
			return new org.deeplearning4j.nn.conf.dropout.AlphaDropout(pSchedule.getBackend());
		}
	}

	@Override public void initializeBackend() {
		p = Double.NaN;
		pSchedule = new ConstantSchedule();
		alpha = org.deeplearning4j.nn.conf.dropout.AlphaDropout.DEFAULT_ALPHA;
		lambda = org.deeplearning4j.nn.conf.dropout.AlphaDropout.DEFAULT_LAMBDA;
		backend = new org.deeplearning4j.nn.conf.dropout.AlphaDropout(p);
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
