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
 * DefaultTokenizerFactoryImpl.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.tokenization.tokenizer.factory.impl;

import java.io.Serializable;

/**
 * A serializable version of DeepLearning4j's DefaultTokenizerFactory.
 *
 * @author Felipe Bravo-Marquez
 */
public class DefaultTokenizerFactoryImpl
		extends org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory implements Serializable {

	/**
	 * For Serialization
	 */
	private static final long serialVersionUID = 762619182220458182L;

}