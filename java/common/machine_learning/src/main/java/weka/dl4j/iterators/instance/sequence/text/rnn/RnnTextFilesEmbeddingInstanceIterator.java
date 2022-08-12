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
 * RnnTextFilesEmbeddingInstanceIterator.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.instance.sequence.text.rnn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.deeplearning4j.iterator.LabeledSentenceProvider;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.InvalidInputDataException;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.dl4j.iterators.dataset.sequence.text.rnn.RnnTextEmbeddingDataSetIterator;
import weka.dl4j.iterators.provider.FileLabeledSentenceProvider;

/**
 * Converts the given Instances object into a DataSet and then constructs and returns a
 * RnnTextEmbeddingInstanceIterator.
 *
 * <p>Assumes the instance object has the following two attributes:
 *
 * <ul>
 * <li>Path to text file
 * <li>Class
 * </ul>
 *
 * @author Steven Lang
 */
public class RnnTextFilesEmbeddingInstanceIterator extends RnnTextEmbeddingInstanceIterator {

	private static final long serialVersionUID = -1065956690877737854L;
	private File textsLocation = new File(System.getProperty("user.dir"));

	@Override public DataSetIterator getDataSetIterator(Instances data, int seed, int batchSize)
			throws InvalidInputDataException, IOException {
		validate(data);
		initWordVectors();
		final LabeledSentenceProvider sentenceProvider = getSentenceProvider(data);
		return new RnnTextEmbeddingDataSetIterator(data, wordVectors, tokenizerFactory, tokenPreProcess, stopwords,
				sentenceProvider, batchSize, truncateLength);
	}

	@Override public LabeledSentenceProvider getSentenceProvider(Instances data) {
		List<File> files = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		final int clsIdx = data.classIndex();
		for (Instance inst : data) {
			labels.add(String.valueOf(inst.value(clsIdx)));
			final String path = inst.stringValue(1 - clsIdx);
			final File file = Paths.get(textsLocation.getAbsolutePath(), path).toFile();
			files.add(file);
		}

		return new FileLabeledSentenceProvider(files, labels, data.numClasses());
	}

	/**
	 * Validates the input dataset
	 *
	 * @param data the input dataset
	 * @throws InvalidInputDataException if validation is unsuccessful
	 */
	public void validate(Instances data) throws InvalidInputDataException {

		if (!getTextsLocation().isDirectory()) {
			throw new InvalidInputDataException("Directory not valid: " + getTextsLocation());
		}
		if (!((data.attribute(0).isString() && data.classIndex() == 1) || (data.attribute(1).isString()
				&& data.classIndex() == 0))) {
			throw new InvalidInputDataException("An ARFF is required with a string attribute and a class attribute");
		}
	}

	@OptionMetadata(displayName = "directory of text files", description = "The directory containing the text files (default = user home).", commandLineParamName = "textsLocation", commandLineParamSynopsis = "-textsLocation <string>", displayOrder = 3) public File getTextsLocation() {
		return textsLocation;
	}

	public void setTextsLocation(File textsLocation) {
		this.textsLocation = textsLocation;
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

	public String globalInfo() {
		return "Text iterator that reads documents from each file that is listed in a meta arff file. "
				+ "Each document is then "
				+ "processed by the tokenization, stopwords, token-preprocessing and afterwards mapped into "
				+ "an embedding space with the given word-vector model.";
	}
}
