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
 * TweetNLPTokenizerFactoryImpl.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.tokenization.tokenizer.factory.impl;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Enumeration;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.dl4j.text.tokenization.tokenizer.TweetNLPTokenizer;

/**
 * A DeepLearning4j's TokenizerFactory interface for the CMU TweetNLP tokenizer.
 *
 * @author Felipe Bravo-Marquez
 */
public class TweetNLPTokenizerFactoryImpl implements TokenizerFactory, Serializable, OptionHandler {

	/**
	 * For Serialization
	 */
	private static final long serialVersionUID = 4694868790645893109L;

	/**
	 * The TokenPreProcess object
	 */
	private TokenPreProcess tokenPreProcess;

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory#create(java.lang.String)
	 */
	@Override public Tokenizer create(String toTokenize) {
		Tokenizer t = new TweetNLPTokenizer(toTokenize);
		t.setTokenPreProcessor(tokenPreProcess);
		return t;
	}

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory#create(java.io.InputStream)
	 */
	@Override public Tokenizer create(InputStream toTokenize) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory#getTokenPreProcessor()
	 */
	@Override public TokenPreProcess getTokenPreProcessor() {
		return tokenPreProcess;
	}

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory#setTokenPreProcessor(org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess)
	 */
	@Override public void setTokenPreProcessor(TokenPreProcess preProcessor) {
		this.tokenPreProcess = preProcessor;
	}

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
}
