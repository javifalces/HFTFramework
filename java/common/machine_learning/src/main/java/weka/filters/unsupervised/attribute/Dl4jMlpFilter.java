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
 * Dl4jMlpFilter.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.filters.unsupervised.attribute;

import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;

import lombok.extern.log4j.Log4j2;
import weka.classifiers.functions.Dl4jMlpClassifier;
import weka.dl4j.Utils;
import weka.core.*;
import weka.dl4j.enums.PoolingType;
import weka.dl4j.iterators.instance.AbstractInstanceIterator;
import weka.dl4j.iterators.instance.DefaultInstanceIterator;
import weka.dl4j.iterators.instance.ImageInstanceIterator;
import weka.dl4j.layers.DenseLayer;
import weka.dl4j.layers.Layer;
import weka.dl4j.zoo.AbstractZooModel;
import weka.dl4j.zoo.Dl4jResNet50;
import weka.filters.Filter;
import weka.filters.SimpleBatchFilter;

/*
<!-- options-start -->
* Valid options are: <p>
*
* <pre> -layer-extract &lt;Dense Layer Spec&gt;
*  Layers used for the feature transformation (can be left blank, default will be applied)</pre>
*
* <pre> -model &lt;File&gt;
*  The trained Dl4jMlpClassifier object that contains the network, used for transformation.</pre>
*
* <pre> -zooModel &lt;Model Zoo specification&gt;
*  The pretrained model from the DL4J Model Zoo (or a keras model)</pre>
*
* <pre> -iterator &lt;string&gt;
*  The instance iterator to use.</pre>
*
* <pre> -poolingType &lt;String&gt;
*  Pooling function to apply on intermediary activations</pre>
*
<!-- options-end -->

<!-- globalinfo-start -->
* Enables the use of a neural network for feature extraction using DL4J.<br>
* One can use either a model that's been trained within WEKA, or simply choose a pretrained model from the model zoo<br>
* (See also https://deeplearning.cms.waikato.ac.nz/user-guide/data/ )
* <br><br>
<!-- globalinfo-end -->

 **/

/**
 * Weka filter that uses a neural network trained via {@link Dl4jMlpClassifier} as feature
 * transformation.
 *
 * @author Steven Lang
 * @author Rhys Compton
 */
@Log4j2 public class Dl4jMlpFilter extends SimpleBatchFilter implements OptionHandler, CapabilitiesHandler {

	/**
	 * Unique ID for version of this class.
	 */
	private static final long serialVersionUID = 1317698787337080580L;
	/**
	 * The classifier model this filter is based on.
	 */
	protected File serializedModelFile = new File(Utils.defaultFileLocation());

	/**
	 * The zoo model to use, if we're not loading from the serialized model file.
	 */
	protected AbstractZooModel zooModelType = new Dl4jResNet50();

	/**
	 * The image instance iterator to use.
	 */
	protected AbstractInstanceIterator instanceIterator = new DefaultInstanceIterator();

	/**
	 * The pooling function to use if taking activations from an intermediary convolution layer
	 * (instead of the already-pooled output layer).
	 */
	protected PoolingType poolingType = PoolingType.MAX;

	/**
	 * Layer names of the layer which is used to get the outputs from.
	 */
	protected DenseLayer[] transformationLayers = new DenseLayer[] {};

	/**
	 * Model used for feature extraction.
	 */
	protected Dl4jMlpClassifier model;

	/**
	 * Flag for the GUI to set whether or not default feature extraction layer should be loaded.
	 */
	protected boolean useDefaultFeatureLayer = true;

	/**
	 * GET/SET METHODS
	 */

	@OptionMetadata(description = "The trained Dl4jMlpClassifier object that contains the network, used for transformation.", displayName = "Serialized model file", commandLineParamName = "model", commandLineParamSynopsis = "-model <File>", displayOrder = 1) public File getSerializedModelFile() {
		return serializedModelFile;
	}

	public void setSerializedModelFile(File modelPath) {
		this.serializedModelFile = modelPath;
	}

	@OptionMetadata(description = "The pretrained model from the DL4J Model Zoo (or a keras model)", displayName = "Pretrained zoo model", commandLineParamName = "zooModel", commandLineParamSynopsis = "-zooModel <Model Zoo specification>", displayOrder = 2) public AbstractZooModel getZooModelType() {
		return zooModelType;
	}

	/**
	 * Clear the old transformation layers and set the new one if we've changed to a different model type.
	 * This function is necessary from a usability perspective - if a user is switching between lots of different models,
	 * the final model's feature extraction layer should be applied. This requires clearing the old layers and applying the new
	 * one. The issue is if a user has selected a zoo model and wants to use some custom layers - when this method is called
	 * by the GUI, it will clear the 'old' layers (which include the custom applied layer) so the user can't actually
	 * use their custom setting.
	 * <p>
	 * To fix this, a GUI flag is shown which the user can set to false if they wish to use non-default feature layers.
	 *
	 * @param zooModelType Type we're changing to
	 */
	public void setZooModelType(AbstractZooModel zooModelType) {
		if (getUseDefaultFeatureLayer()) {
			log.warn("Using default feature layer, so clearing user-specified transformation layers. "
					+ "If you wanted to keep them you will need to set this flag to false and set them again.");
			clearTransformationLayers();
			addTransformationLayerName(zooModelType.getFeatureExtractionLayer());
		} else {
			log.warn("'-default-feature-layer' flag is not set. Please ensure you have explicitly defined the feature "
					+ "extraction layers. If you wish to use the default feature extraction layer, append the -default-feature-layer param.");
		}
		this.zooModelType = zooModelType;
		// Also set the instance iterator to use this zoo model's channel order
		if (this.instanceIterator instanceof ImageInstanceIterator) {
			((ImageInstanceIterator) this.instanceIterator).setChannelsLast(this.zooModelType.getChannelsLast());
		}
	}

	@OptionMetadata(description = "The instance iterator to use.", displayName = "instance iterator", commandLineParamName = "iterator", commandLineParamSynopsis = "-iterator <string>") public AbstractInstanceIterator getInstanceIterator() {
		return instanceIterator;
	}

	public void setInstanceIterator(AbstractInstanceIterator instanceIterator) {
		this.instanceIterator = instanceIterator;
	}

	@OptionMetadata(description = "Pooling function to apply on intermediary activations", displayName = "Pooling Type", commandLineParamName = "poolingType", commandLineParamSynopsis = "-poolingType <String>") public PoolingType getPoolingType() {
		return poolingType;
	}

	public void setPoolingType(PoolingType poolingType) {
		this.poolingType = poolingType;
	}

	@OptionMetadata(description = "Layers used for the feature transformation (can be left blank, default will be applied)", displayName = "Feature extraction layers", commandLineParamName = "layer-extract", commandLineParamSynopsis = "-layer-extract <Dense Layer Spec>", displayOrder = 0) public DenseLayer[] getTransformationLayers() {
		return transformationLayers;
	}

	public DenseLayer getTransformationLayer(int index) {
		return transformationLayers[index];
	}

	public void setTransformationLayers(DenseLayer[] transformationLayers) {
		this.transformationLayers = transformationLayers;
	}

	public void setTransformationLayerNames(String[] transformationLayerNames) {
		this.transformationLayers = Arrays.stream(transformationLayerNames).map(x -> {
			DenseLayer newLayer = new DenseLayer();
			newLayer.setLayerName(x);
			return newLayer;
		}).toArray(DenseLayer[]::new);
	}

	/**
	 * Adds a new transformation layer for the filter to use.
	 *
	 * @param transformationLayerName name of the layer in the model to take activations from
	 */
	public void addTransformationLayerName(String transformationLayerName) {
		int n = this.transformationLayers.length;
		DenseLayer[] newArr = new DenseLayer[n + 1];
		System.arraycopy(this.transformationLayers, 0, newArr, 0, n);

		DenseLayer newLayer = new DenseLayer();
		newLayer.setLayerName(transformationLayerName);
		newArr[n] = newLayer;
		this.transformationLayers = newArr;
	}

	/**
	 * Clear the transformation layers to be used by the filter.
	 */
	public void clearTransformationLayers() {
		this.transformationLayers = new DenseLayer[] {};
	}

	public boolean getUseDefaultFeatureLayer() {
		return useDefaultFeatureLayer;
	}

	@OptionMetadata(displayName = "Use default feature layer", description = "Set to true to load the default feature extraction layer in the GUI - false to set your own custom layers.", displayOrder = 0, commandLineParamName = "default-feature-layer", commandLineParamSynopsis = "-default-feature-layer", commandLineParamIsFlag = true) public void setUseDefaultFeatureLayer(
			boolean useDefaultFeatureLayer) {
		this.useDefaultFeatureLayer = useDefaultFeatureLayer;
	}

	/**
	 * FILTER CODE.
	 */

	public Dl4jMlpFilter() {
		// By default we set the zoo model default feature extraction as our layer to use
		addTransformationLayerName(zooModelType.getFeatureExtractionLayer());
	}

	@Override public boolean allowAccessToFullInputFormat() {
		return true;
	}

	/**
	 * Load the model on the data.
	 *
	 * @param data Sets up the filter by loading the model (either from file or from model zoo)
	 * @throws Exception From errors occurring during loading the model file, or from intializing from the data
	 */
	private void loadModel(Instances data) throws Exception {
		model = Dl4jMlpClassifier.loadModel(data, serializedModelFile, zooModelType, instanceIterator);
	}

	/**
	 * Return transformation layer names.
	 *
	 * @return String[] containing the names of transformation layers this filter is using
	 */
	public String[] transformationLayersToNames() {
		return Arrays.stream(transformationLayers).map(Layer::getLayerName).toArray(String[]::new);
	}

	@Override protected Instances determineOutputFormat(Instances inputFormat) throws Exception {
		loadModel(inputFormat);
		// No need to featurize full dataset at this point - only getting the output format
		Instances subset = new Instances(inputFormat, 0, 1);
		return model.getActivationsAtLayers(transformationLayersToNames(), subset, poolingType);
	}

	@Override protected Instances process(Instances instances) throws Exception {
		return model.getActivationsAtLayers(transformationLayersToNames(), instances, poolingType);
	}

	@Override public String globalInfo() {
		return "Enables the use of a neural network for feature extraction using DL4J.\n"
				+ "One can use either a model that's been trained within WEKA, or simply choose a pretrained model from the model zoo\n"
				+ "(See also https://deeplearning.cms.waikato.ac.nz/user-guide/data/ )";
	}

	/**
	 * Returns default capabilities of the classifier.
	 *
	 * @return the capabilities of this classifier
	 */
	@Override public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();

		// attributes
		result.enable(Capabilities.Capability.STRING_ATTRIBUTES);
		result.enable(Capabilities.Capability.NUMERIC_ATTRIBUTES);

		// class
		result.enable(Capabilities.Capability.NOMINAL_CLASS);
		result.enable(Capabilities.Capability.NUMERIC_CLASS);
		result.enable(Capabilities.Capability.DATE_CLASS);
		result.enable(Capabilities.Capability.MISSING_CLASS_VALUES);

		return result;
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration<Option> listOptions() {
		return Option.listOptionsForClassHierarchy(this.getClass(), Filter.class).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {
		return Option.getOptionsForHierarchy(this, Filter.class);
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		Option.setOptionsForHierarchy(options, this, Filter.class);
	}
}
