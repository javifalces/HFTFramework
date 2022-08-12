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
 * FileLabeledSentenceProvider.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.provider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.deeplearning4j.iterator.LabeledSentenceProvider;
import org.nd4j.common.primitives.Pair;

/**
 * Implement LabeledSentenceProvider for loading labeled files.
 *
 * @author Steven Lang
 */
@Log4j2 public class FileLabeledSentenceProvider implements LabeledSentenceProvider {

	private List<File> files;
	private List<String> labels;
	private List<String> allLabels;
	private int numClasses;
	private int cursor;

	public FileLabeledSentenceProvider(List<File> files, List<String> labels, int numClasses) {
		this.files = files;
		this.labels = labels;
		this.allLabels = new ArrayList<>(new HashSet<>(labels));
		this.numClasses = numClasses;
		this.cursor = 0;
	}

	@Override public boolean hasNext() {
		return cursor < totalNumSentences();
	}

	@Override public Pair<String, String> nextSentence() {
		final String label = labels.get(cursor);
		final File file = files.get(cursor);

		String sentence;
		try {
			sentence = FileUtils.readFileToString(file);
		} catch (IOException e) {
			log.error("File not found: " + file, e);
			throw new RuntimeException("File not found: " + file);
		}

		cursor++;
		return new Pair<>(sentence, label);
	}

	@Override public void reset() {
		cursor = 0;
	}

	@Override public int totalNumSentences() {
		return files.size();
	}

	@Override public List<String> allLabels() {
		return allLabels;
	}

	@Override public int numLabelClasses() {
		return numClasses;
	}
}