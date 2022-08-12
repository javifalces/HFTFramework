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
 * Dl4jAbstractFileBasedStopwords.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.text.stopwords;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import weka.core.Option;
import weka.core.Utils;

/**
 * Abstract File based stopwords handler.
 *
 * @author fracpete, Felipe Bravo-Marquez
 */
abstract class Dl4jAbstractFileBasedStopwords extends Dl4jAbstractStopwords {

	/**
	 * for serialization
	 */
	private static final long serialVersionUID = 3733779108023241551L;

	/**
	 * a file containing stopwords.
	 */
	protected File stopWordsFile = new File(System.getProperty("user.dir"));

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override public Enumeration<Option> listOptions() {
		Vector<Option> result = new Vector<Option>();

		Enumeration<Option> enm = super.listOptions();
		while (enm.hasMoreElements()) {
			result.add(enm.nextElement());
		}

		result.addElement(
				new Option("\t" + stopwordsTipText() + "\n" + "\t(default: .)", "stopwords", 1, "-stopwords <file>"));

		return result.elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override public String[] getOptions() {
		List<String> options = new ArrayList<String>(Arrays.asList(super.getOptions()));

		options.add("-stopwords");
		options.add(getStopwords().toString());

		return options.toArray(new String[options.size()]);
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	@Override public void setOptions(String[] options) throws Exception {
		String tmpStr;

		tmpStr = Utils.getOption("stopwords", options);
		if (tmpStr.isEmpty()) {
			setStopwords(new File("."));
		} else {
			setStopwords(new File(tmpStr));
		}

		super.setOptions(options);
	}

	/**
	 * returns the file used for obtaining the stopwords, if the file represents a directory then the
	 * default ones are used.
	 *
	 * @return the file containing the stopwords
	 */
	public File getStopwords() {
		return stopWordsFile;
	}

	/**
	 * Sets the file containing the stopwords, null or a directory unset the stopwords.
	 *
	 * @param value the file containing the stopwords
	 */
	public void setStopwords(File value) {
		if (value == null) {
			value = new File(System.getProperty("user.dir"));
		}

		stopWordsFile = value;
		reset();
	}

	/**
	 * Returns the tip text for this property.
	 *
	 * @return tip text for this property suitable for displaying in the explorer/experimenter gui
	 */
	public abstract String stopwordsTipText();

	/**
	 * Reads in the stopwords file, line by line (trimmed). Returns an empty list if not existing or a
	 * directory.
	 *
	 * @return the content of the file
	 */
	protected List<String> read() {
		List<String> result;
		String line;
		BufferedReader reader;

		result = new ArrayList<String>();

		if (stopWordsFile.exists() && !stopWordsFile.isDirectory()) {
			reader = null;
			try {
				reader = new BufferedReader(new FileReader(stopWordsFile));
				while ((line = reader.readLine()) != null) {
					result.add(line.trim());
				}
			} catch (Exception e) {
				error("Failed to read stopwords file '" + stopWordsFile + "'!");
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception ex) {
						// ignored
					}
				}
			}
		}

		return result;
	}
}
