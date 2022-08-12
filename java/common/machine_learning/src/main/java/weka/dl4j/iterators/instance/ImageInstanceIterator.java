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
 * ImageInstanceIterator.java
 * Copyright (C) 2016-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.iterators.instance;

import java.io.File;
import java.util.Enumeration;

import lombok.extern.log4j.Log4j2;
import org.datavec.api.split.CollectionInputSplit;
import org.datavec.image.recordreader.ImageRecordReader;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.core.Environment;
import weka.core.Instances;
import weka.core.InvalidInputDataException;
import weka.core.Option;
import weka.core.OptionMetadata;
import weka.dl4j.ArffMetaDataLabelGenerator;
import weka.dl4j.enums.PretrainedType;
import weka.dl4j.iterators.instance.api.ConvolutionalIterator;
import weka.dl4j.zoo.AbstractZooModel;
import weka.dl4j.zoo.Dl4jLeNet;
import weka.gui.FilePropertyMetadata;
import weka.gui.knowledgeflow.KFGUIConsts;

/**
 * An iterator that loads images.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 * @author Steven Lang
 */
@Log4j2 public class ImageInstanceIterator extends AbstractInstanceIterator implements ConvolutionalIterator {

	/**
	 * The version ID used for serializing objects of this class.
	 */
	private static final long serialVersionUID = -3701309032945158130L;

	/**
	 * The desired output height.
	 */
	protected int height = 28;

	/**
	 * The desired output width.
	 */
	protected int width = 28;

	/**
	 * The desired number of channels.
	 */
	protected int numChannels = 1;

	/**
	 * If true, swap the reader to supply image channels last.
	 */
	protected boolean channelsLast = false;

	/**
	 * The location of the folder containing the images.
	 */
	protected File imagesLocation = new File(System.getProperty("user.dir"));

	@FilePropertyMetadata(fileChooserDialogType = KFGUIConsts.SAVE_DIALOG, directoriesOnly = true) @OptionMetadata(displayName = "directory of images", description = "The directory containing the images (default = user home).", commandLineParamName = "imagesLocation", commandLineParamSynopsis = "-imagesLocation <string>", displayOrder = 1) public File getImagesLocation() {
		return imagesLocation;
	}

	public void setImagesLocation(File imagesLocation) {
		this.imagesLocation = imagesLocation;
	}

	@OptionMetadata(displayName = "desired width", description = "The desired width of the images (default = 28).", commandLineParamName = "width", commandLineParamSynopsis = "-width <int>", displayOrder = 2) public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	@OptionMetadata(displayName = "desired height", description = "The desired height of the images (default = 28).", commandLineParamName = "height", commandLineParamSynopsis = "-height <int>", displayOrder = 3) public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	@OptionMetadata(displayName = "desired number of channels", description = "The desired number of channels (default = 1).", commandLineParamName = "numChannels", commandLineParamSynopsis = "-numChannels <int>", displayOrder = 4) public int getNumChannels() {
		return numChannels;
	}

	public void setNumChannels(int numChannels) {
		this.numChannels = numChannels;
	}

	@OptionMetadata(displayName = "Image channels last", description = "Set to true to supply image channels last. "
			+ "The default value will usually be correct, so as an end user you shouldn't need to change this setting. "
			+ "If you do be aware that it may break the model.", commandLineParamName = "channelsLast", commandLineParamSynopsis = "-channelsLast <boolean>") public boolean getChannelsLast() {
		return channelsLast;
	}

	public void setChannelsLast(boolean channelsLast) {
		this.channelsLast = channelsLast;
	}

	/**
	 * Enforces the input image size if using a zoo model.
	 *
	 * @param tmpZooModel Zoo model to constrain input size to
	 */
	public void enforceZooModelSize(AbstractZooModel tmpZooModel) {
		// https://deeplearning4j.konduit.ai/model-zoo/overview#changing-inputs
		if (tmpZooModel.isPretrained()) {
			if (tmpZooModel instanceof Dl4jLeNet && tmpZooModel.getPretrainedType() == PretrainedType.MNIST) {
				log.warn("Using LeNet with MNIST weights, setting shape to 1, 28, 28");
				setNumChannels(1);
				setHeight(28);
				setWidth(28);
			} else {
				int[] pretrainedShape = tmpZooModel.getInputShape();
				setNumChannels(pretrainedShape[0]);
				setHeight(pretrainedShape[1]);
				setWidth(pretrainedShape[2]);
			}
			log.warn(String.format("Using pretrained model weights, setting shape to: %d, %d, %d", getNumChannels(),
					getWidth(), getHeight()));
		}
	}

	/**
	 * Validates the input dataset.
	 *
	 * @param data the input dataset
	 * @throws InvalidInputDataException if validation is unsuccessful
	 */
	public void validate(Instances data) throws InvalidInputDataException {
		Environment env = Environment.getSystemWide();
		String resolved = getImagesLocation().toString();
		try {
			resolved = env.substitute(getImagesLocation().toString());
		} catch (Exception ex) {
			// ignore
		}
		File imagesLoc = new File(resolved);
		if (!imagesLoc.isDirectory()) {
			throw new InvalidInputDataException("Directory not valid: " + resolved);
		}
		if (!isMetaArff(data)) {
			throw new InvalidInputDataException("An ARFF is required with a string attribute and a class attribute");
		}
	}

	/**
	 * Are the input instances from a 'meta' arff (just points to the image location)
	 *
	 * @param data Instances to verify
	 * @return true if Instances are meta
	 */
	public static boolean isMetaArff(Instances data) {
		return (data.attribute(0).isString() && data.classIndex() == 1);
	}

	/**
	 * Returns the image recorder.
	 *
	 * @param data the dataset to use
	 * @return the image recorder
	 */
	protected ImageRecordReader getImageRecordReader(Instances data) throws Exception {
		Environment env = Environment.getSystemWide();
		String resolved = getImagesLocation().toString();
		try {
			resolved = env.substitute(getImagesLocation().toString());
		} catch (Exception ex) {
			// ignore
		}

		ArffMetaDataLabelGenerator labelGenerator = new ArffMetaDataLabelGenerator(data, resolved);
		ImageRecordReader reader = new ImageRecordReader(getHeight(), getWidth(), getNumChannels(), labelGenerator);
		CollectionInputSplit cis = new CollectionInputSplit(labelGenerator.getPathURIs());
		reader.initialize(cis);

		return reader;
	}

	/**
	 * This method returns the iterator. Scales all intensity values: it divides them by 255.
	 *
	 * @param data      the dataset to use
	 * @param seed      the seed for the random number generator
	 * @param batchSize the batch size to use
	 * @return the iterator
	 */
	@Override public DataSetIterator getDataSetIterator(Instances data, int seed, int batchSize) throws Exception {

		batchSize = Math.min(data.numInstances(), batchSize);
		validate(data);
		ImageRecordReader reader = getImageRecordReader(data);

		// Required for supporting channels-last models (currently only EfficientNet)
		if (getChannelsLast())
			reader.setNchw_channels_first(false);

		final int labelIndex = 1; // Use explicit label index position
		final int numPossibleLabels = data.numClasses();
		DataSetIterator tmpIter = new RecordReaderDataSetIterator(reader, batchSize, labelIndex, numPossibleLabels);
		DataNormalization scaler = new ImagePreProcessingScaler(0, 1);
		scaler.fit(tmpIter);
		tmpIter.setPreProcessor(scaler);
		return tmpIter;
	}

	/**
	 * Return the global info for this class.
	 *
	 * @return Global info
	 */
	public String globalInfo() {
		return "Instance iterator that reads images based on the meta-data " + "given in the ARFF file.";
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
