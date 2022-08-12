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
 * ArffMetaDataLabelGenerator.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.datavec.api.io.labels.PathLabelGenerator;
import org.datavec.api.writable.Text;
import org.datavec.api.writable.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.classifiers.functions.Dl4jMlpClassifier;
import weka.core.Instance;
import weka.core.Instances;

/**
 * A LabelGenerator based on the file path in the Arff-Meta dataset
 *
 * @author Steven Lang
 */
public class ArffMetaDataLabelGenerator implements PathLabelGenerator {

	private static final long serialVersionUID = 5918725700616180163L;

	/**
	 * The logger used in this class.
	 */
	protected final Logger log = LoggerFactory.getLogger(Dl4jMlpClassifier.class);

	/**
	 * Mapping from paths to labels
	 */
	private Map<String, String> fileLabelMap;

	/**
	 * Paths of the input files
	 */
	private List<String> paths;

	/**
	 * Labels of the input files
	 */
	private List<String> labels;

	/**
	 * Basepath where the files reside
	 */
	private String basePath;

	/**
	 * Default constructor which sets the metaData
	 *
	 * @param metaData Meta data with mapping: filename to label
	 * @param path     Directory path
	 */
	public ArffMetaDataLabelGenerator(Instances metaData, String path) {

		// If this path is absolute set it as basepath
		if (new File(path).isAbsolute()) {
			this.basePath = path;
		} else {
			String currentPath = Paths.get(System.getProperty("user.dir")).toString();
			this.basePath = Paths.get(currentPath, path).toString();
		}

		// Fill mapping from image path to
		fileLabelMap = new TreeMap<>();
		paths = new ArrayList<>();
		labels = new ArrayList<>();
		for (Instance inst : metaData) {
			String fileName = inst.stringValue(0);
			String label = inst.stringValue(1);
			String absPath = Paths.get(this.basePath, fileName).toFile().getAbsolutePath();
			paths.add(absPath);
			labels.add(label);
			fileLabelMap.put(absPath, label);
		}
	}

	/**
	 * Select the label based on the path in the metadata
	 *
	 * @param path Input path to the image
	 * @return Label of the image at the given path
	 */
	@Override public Writable getLabelForPath(String path) {
		String absPath = new File(path).getAbsolutePath();
		final String label = fileLabelMap.get(absPath);
		return new Text(label);
	}

	@Override public Writable getLabelForPath(URI uri) {
		return getLabelForPath(uri.getPath());
	}

	@Override public boolean inferLabelClasses() {
		return true;
	}

	/**
	 * Get all paths as uris
	 *
	 * @return Collection of uri paths
	 */
	public Collection<URI> getPathURIs() {
		return paths.stream().map(s -> Paths.get(s).toUri()).collect(Collectors.toList());
	}
}
