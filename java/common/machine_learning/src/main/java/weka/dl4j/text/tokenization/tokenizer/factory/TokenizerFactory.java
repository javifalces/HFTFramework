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
 * TokenizerFactory.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.tokenization.tokenizer.factory;

import java.io.Serializable;
import java.util.Enumeration;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.dl4j.ApiWrapper;
import weka.dl4j.ApiWrapperUtil;

/**
 * TokenizerFactory wrapper for Deeplearning4j's {@link org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory}
 * classes.
 *
 * @author Steven Lang
 */
@EqualsAndHashCode @ToString public abstract class TokenizerFactory<T extends org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory>
		implements OptionHandler, ApiWrapper<T>, Serializable {

	private static final long serialVersionUID = 4013807949829115985L;
	/**
	 * TokenizerFactory that is backing the implementation
	 */
	T backend;

	public TokenizerFactory() {
		initializeBackend();
	}

	/**
	 * Create an API wrapped schedule from a given ISchedule object.
	 *
	 * @param newBackend Backend object
	 * @return API wrapped object
	 */
	public static TokenizerFactory<? extends org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory> create(
			org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory newBackend) {
		return ApiWrapperUtil.getImplementingWrapper(TokenizerFactory.class, newBackend,
				"weka.dl4j.text.tokenization.tokenizer.factory");
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
