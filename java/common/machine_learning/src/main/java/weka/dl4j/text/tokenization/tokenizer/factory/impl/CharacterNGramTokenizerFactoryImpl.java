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
 * CharacterNGramTokenizerFactoryImpl.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.tokenization.tokenizer.factory.impl;

import java.io.InputStream;
import java.io.Serializable;

import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import weka.core.tokenizers.CharacterNGramTokenizer;
import weka.dl4j.text.tokenization.tokenizer.WekaTokenizer;

/**
 * A DeepLearning4j's TokenizerFactory interface for Weka core tokenizers.
 *
 * @author Felipe Bravo-Marquez
 */
public class CharacterNGramTokenizerFactoryImpl implements TokenizerFactory, Serializable {

	/**
	 * For Serialization
	 */
	private static final long serialVersionUID = 4694868790645893109L;
	/**
	 * the maximum number of N
	 */
	protected int nMax = 3;
	/**
	 * the minimum number of N
	 */
	protected int nMin = 1;
	/**
	 * The TokenPreProcess object
	 */
	private TokenPreProcess tokenPreProcess;
	private CharacterNGramTokenizer wekaTokenizer;

	/* (non-Javadoc)
	 * @see org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory#create(java.lang.String)
	 */
	@Override public Tokenizer create(String toTokenize) {

		this.wekaTokenizer = new CharacterNGramTokenizer();
		this.wekaTokenizer.setNGramMinSize(this.nMin);
		this.wekaTokenizer.setNGramMaxSize(this.nMax);

		WekaTokenizer t = new WekaTokenizer(toTokenize, wekaTokenizer);
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

	public int getNMax() {
		return nMax;
	}

	public void setNMax(int nMax) {
		this.nMax = nMax;
	}

	public int getNMin() {
		return nMin;
	}

	public void setNMin(int nMin) {
		this.nMin = nMin;
	}
}
