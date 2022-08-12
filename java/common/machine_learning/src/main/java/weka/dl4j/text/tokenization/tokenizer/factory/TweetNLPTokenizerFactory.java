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
 * TweetNLPTokenizerFactory.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.tokenization.tokenizer.factory;

import java.io.Serializable;
import java.util.Enumeration;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.dl4j.text.tokenization.tokenizer.factory.impl.TweetNLPTokenizerFactoryImpl;

/**
 * A DeepLearning4j's TokenizerFactory interface for the CMU TweetNLP tokenizer.
 *
 * @author Felipe Bravo-Marquez
 */
public class TweetNLPTokenizerFactory extends TokenizerFactory<TweetNLPTokenizerFactoryImpl>
		implements Serializable, OptionHandler {

	private static final long serialVersionUID = -4219961945839846195L;

	/**
	 * Returns a string describing this object.
	 *
	 * @return a description of the object suitable for displaying in the explorer/experimenter gui
	 */
	public String globalInfo() {
		return "Uses the CMU TweetNLP tokenizer.";
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

	@Override public void initializeBackend() {
		backend = new TweetNLPTokenizerFactoryImpl();
	}
}
