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
 * LossFunction.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.lossfunctions;

import java.io.Serializable;
import java.util.Enumeration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.dl4j.ApiWrapper;
import weka.dl4j.ApiWrapperUtil;

@EqualsAndHashCode @ToString public abstract class LossFunction<T extends ILossFunction>
		implements ApiWrapper<T>, OptionHandler, Serializable {

	private static final long serialVersionUID = 7293731657945995479L;
	T backend;

	public LossFunction() {
		initializeBackend();
	}

	/**
	 * Create an API wrapped schedule from a given ILossFunction object.
	 *
	 * @param newBackend Backend object
	 * @return API wrapped object
	 */
	public static LossFunction<? extends ILossFunction> create(ILossFunction newBackend) {
		return (LossFunction<? extends ILossFunction>) ApiWrapperUtil
				.getImplementingWrapper(LossFunction.class, newBackend, "weka.dl4j.lossfunctions");
	}

	@Override public T getBackend() {
		return backend;
	}

	@Override public void setBackend(T newBackend) {
		backend = newBackend;
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
