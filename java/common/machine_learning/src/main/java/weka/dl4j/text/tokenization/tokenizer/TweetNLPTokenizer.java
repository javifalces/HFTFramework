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
 * TweetNLPTokenizer.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.tokenization.tokenizer;

import cmu.arktweetnlp.Twokenize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;

/**
 * A DeepLearning4j's Tokenizer interface for the CMU TweetNLP tokenizer.
 *
 * @author Felipe Bravo-Marquez
 */
public class TweetNLPTokenizer implements Tokenizer, Serializable {

	private static final long serialVersionUID = 4313371978509941373L;
	/**
	 * The TokenPreProcess Object
	 */
	private TokenPreProcess tokenPreProcess;

	/**
	 * The list of tokenized tokens
	 */
	private List<String> tokens;

	/**
	 * The String iterator
	 */
	private Iterator<String> iterator;

	/**
	 * initializes the Object
	 *
	 * @param content the String to tokenize
	 */
	public TweetNLPTokenizer(String content) {
		this.tokens = Twokenize.tokenizeRawTweetText(content);
		this.iterator = tokens.iterator();
	}

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizer.Tokenizer#countTokens()
	 */
	@Override public int countTokens() {
		return this.tokens.size();
	}

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizer.Tokenizer#getTokens()
	 */
	@Override public List<String> getTokens() {
		List<String> tokens = new ArrayList<String>();
		while (hasMoreTokens()) {
			tokens.add(nextToken());
		}
		return tokens;
	}

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizer.Tokenizer#setTokenPreProcessor(org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess)
	 */
	@Override public void setTokenPreProcessor(TokenPreProcess tokenPreProcessor) {
		this.tokenPreProcess = tokenPreProcessor;
	}

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizer.Tokenizer#hasMoreTokens()
	 */
	@Override public boolean hasMoreTokens() {
		return this.iterator.hasNext();
	}

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizer.Tokenizer#nextToken()
	 */
	@Override public String nextToken() {

		String base = this.iterator.next();
		if (tokenPreProcess != null) {
			base = tokenPreProcess.preProcess(base);
		}
		return base;
	}
}
