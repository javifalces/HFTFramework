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
 * StemmingPreProcessorImpl.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.tokenization.preprocessor.impl;

import java.io.Serializable;
import java.util.Enumeration;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.stemmers.NullStemmer;
import weka.core.stemmers.Stemmer;

/**
 * Implements basic cleaning inherited from CommonPreProcessor + does stemming using a Weka
 * Stemmer.
 *
 * @author Felipe Bravo-Marquez
 */
public class StemmingPreProcessorImpl
		extends org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor
		implements Serializable, OptionHandler {

	/**
	 * For serialization
	 */
	private static final long serialVersionUID = 436336311776463684L;

	/**
	 * A Weka stemmer objet
	 */
	private Stemmer stemmer = new NullStemmer();

	/**
	 * Returns a string describing this object.
	 *
	 * @return a description of the object suitable for displaying in the explorer/experimenter gui
	 */
	public String globalInfo() {
		return "This tokenizer preprocessor implements basic cleaning inherited from CommonPreProcessor + does stemming using a Weka Stemmer.\n";
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

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreProcessor#preProcess(java.lang.String)
	 */
	@Override public String preProcess(String token) {
		String prep = super.preProcess(token);
		return stemmer.stem(prep);
	}

	@OptionMetadata(displayName = "stemmer", description = "The Weka stemmer to use.", commandLineParamName = "stemmer", commandLineParamSynopsis = "-stemmer <String>", displayOrder = 0) public Stemmer getStemmer() {
		return stemmer;
	}

	public void setStemmer(Stemmer stemmer) {
		this.stemmer = stemmer;
	}
}