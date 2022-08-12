package weka.dl4j.inference;

import lombok.extern.log4j.Log4j2;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.classifiers.functions.Dl4jMlpClassifier;
import weka.core.progress.ProgressManager;
import weka.dl4j.Utils;
import weka.core.*;
import weka.dl4j.interpretability.AbstractCNNSaliencyMapWrapper;
import weka.dl4j.interpretability.WekaScoreCAM;
import weka.dl4j.zoo.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.Enumeration;

/**
 * Tool to allow easy experimentation and exploration of a trained ComputationGraph - either from a previously trained
 * Dl4jMlpClassifier, or from a pretrained Zoo Model (default).
 *
 * @author - Rhys Compton
 */
@Log4j2 public class Dl4jCNNExplorer implements Serializable, OptionHandler, CommandlineRunnable {

	/**
	 * Set to true to use the serialized model file, false otherwise.
	 */
	protected boolean useCustomModel = false;

	/**
	 * Custom model setup, used if the model is supplying a custom-trained model.
	 */
	protected CustomModelSetup customModelSetup = new CustomModelSetup();

	/**
	 * The zoo model to use, if we're not loading from the serialized model file (default).
	 */
	protected AbstractZooModel zooModelType = new Dl4jResNet50();

	/**
	 * Decodes the prediction IDs to human-readable format
	 * Defaults to a decoder for IMAGENET classes.
	 */
	protected ModelOutputDecoder modelOutputDecoder = new ModelOutputDecoder();

	/**
	 * Flag for ScoreCAM saliency map generation.
	 */
	protected boolean generateSaliencyMap = false;

	/**
	 * Class to generate a saliency map.
	 */

	protected AbstractCNNSaliencyMapWrapper saliencyMapWrapper = new WekaScoreCAM();
	/**
	 * Model used for feature extraction.
	 */
	protected Dl4jMlpClassifier model;

	/**
	 * Predictions for the current image.
	 */
	protected TopNPredictions currentPredictions;

	/**
	 * Initialize the ComputationGraph.
	 *
	 * @throws Exception Exceptions from loading the ComputationGraph
	 */
	public void init() throws Exception {
		checkArgs();
		model = Dl4jMlpClassifier.loadInferenceModel(customModelSetup.getSerializedModelFile(), zooModelType);
	}

	/**
	 * Check the arguments for the explorer.
	 *
	 * @throws WekaException If the user hasn't selected a model file
	 */
	public void checkArgs() throws WekaException {
		// If the user has set use-custom-model to True but not selected their model file
		if (useCustomModel && !Utils.notDefaultFileLocation(customModelSetup.getSerializedModelFile()))
			throw new WekaException("If using a custom model setup, you must select the model file location");
	}

	/**
	 * Checks whether the user has selected the correct ImageNet classmap for Darknet, otherwise will return incorrect
	 * class names.
	 *
	 * @return True if the classmap is correctly set.
	 */
	private boolean correctDarknetClassmap() {
		boolean isDarknet = model.getZooModel() instanceof Dl4jDarknet19;
		// Don't need to check if we're not using darknet
		if (!isDarknet)
			return true;

		boolean isPretrained = model.getZooModel().isPretrained();
		// Don't need to check if we're not using a pretrained Darknet
		if (!isPretrained)
			return true;

		return modelOutputDecoder.getBuiltInClassMap() == ClassmapType.DARKNET_IMAGENET;
	}

	/**
	 * Performs prediction and (optionally) computes a saliency map.
	 *
	 * @param imageFile Image to compute.
	 * @throws Exception Exception during prediction.
	 */
	public void processImage(File imageFile) throws Exception {
		// Load the image
		InputType.InputTypeConvolutional inputShape = model.getInputShape(getCustomModelSetup());
		NativeImageLoader loader = new NativeImageLoader(inputShape.getHeight(), inputShape.getWidth(),
				inputShape.getChannels());
		INDArray image = loader.asMatrix(imageFile);

		// We may need to change the channel order if using a channelsLast model (e.g., EfficientNet)
		if (model.getZooModel().getChannelsLast()) {
			log.info("Permuting channel order of input image...");
			image = image.permute(0, 2, 3, 1);
		}

		if (model.getZooModel().requiresPreProcessing()) {
			log.info("Applying image preprocessing...");
			ImagePreProcessingScaler preprocessor = model.getZooModel().getImagePreprocessingScaler();
			preprocessor.transform(image);
		}

		if (!correctDarknetClassmap()) {
			throw new IllegalArgumentException(
					"You have selected the Darknet19 model but aren't using the DARKNET_IMAGENET classmap. "
							+ "Please be aware that the class mapping is different for the pretrained Darknet model, so "
							+ "you may get erroneous class predictions (class names seem incorrect).");
		}

		// Run prediction
		INDArray result = model.outputSingle(image.dup());

		// Decode and store the predictions
		currentPredictions = modelOutputDecoder.decodePredictions(result, imageFile.getName(), getModelName());

		if (!getGenerateSaliencyMap()) {
			log.debug("No saliency map generated");
			return;
		}

		log.info("Generating saliency map...");
		saliencyMapWrapper.setDl4jMlpClassifier(model);
		saliencyMapWrapper.setCustomModelSetup(getCustomModelSetup());
		saliencyMapWrapper.setClassMap(modelOutputDecoder.getClasses());
		saliencyMapWrapper.processImage(imageFile);
	}

	/**
	 * Generate and return the heatmap.
	 *
	 * @return Generated heatmap.
	 */
	public BufferedImage generateOutputMap() {
		return saliencyMapWrapper.generateHeatmapToImage();
	}

	/**
	 * Generate the output heatmap, and save to file.
	 */
	public void generateAndSaveOutputMap() {
		BufferedImage output = generateOutputMap();
		saliencyMapWrapper.saveResult(output);
	}

	/**
	 * Get the name of the loaded model
	 *
	 * @return Model name
	 */
	public String getModelName() {
		return model.getModelName();
	}

	public TopNPredictions getCurrentPredictions() {
		return currentPredictions;
	}

	/**
	 * Close up all progress managers when we finish processing.
	 */
	public void finishProgress() {
		AbstractCNNSaliencyMapWrapper wrapper = getSaliencyMapWrapper();
		if (wrapper == null)
			return;

		ProgressManager progressManager = wrapper.getProgressManager();
		if (progressManager == null)
			return;

		progressManager.finish();
	}

	/* Getters and setters */

	@OptionMetadata(commandLineParamIsFlag = true, commandLineParamName = "useCustomModel", commandLineParamSynopsis = "-useCustomModel", displayName = "Use custom-trained model file", description = "Use the a custom-trained Dl4jMlpClassifier serialized model file, instead of the zoo model.", displayOrder = 0) public boolean getUseCustomModel() {
		return useCustomModel;
	}

	public void setUseCustomModel(boolean useCustomModel) {
		this.useCustomModel = useCustomModel;
		getCustomModelSetup().setUseCustomSetup(useCustomModel);
	}

	@OptionMetadata(displayName = "Custom model setup", description = "Options to set if using a custom-trained model", commandLineParamName = "custom-model", commandLineParamSynopsis = "-customModel <options>", displayOrder = 1) public CustomModelSetup getCustomModelSetup() {
		return customModelSetup;
	}

	public void setCustomModelSetup(CustomModelSetup customModelSetup) {
		this.customModelSetup = customModelSetup;
	}

	@OptionMetadata(displayName = "Pretrained zoo model", description = "Type of pretrained model to use for prediction (instead of trained Dl4jMlpClassifier)", commandLineParamName = "zooModel", commandLineParamSynopsis = "-zooModel <options>", displayOrder = 2) public AbstractZooModel getZooModelType() {
		return zooModelType;
	}

	public void setZooModelType(AbstractZooModel zooModelType) {
		this.zooModelType = zooModelType;
	}

	@OptionMetadata(displayName = "Model output decoder", description = "Handles decoding of the model predictions", commandLineParamName = "decoder", commandLineParamSynopsis = "-decoder <options>", displayOrder = 3) public ModelOutputDecoder getModelOutputDecoder() {
		return modelOutputDecoder;
	}

	public void setModelOutputDecoder(ModelOutputDecoder modelOutputDecoder) {
		this.modelOutputDecoder = modelOutputDecoder;
	}

	@OptionMetadata(displayName = "Generate saliency map", description = "Should the model explorer generate a ScoreCAM saliency map?", commandLineParamName = "generate-map", commandLineParamSynopsis = "-generate-map", commandLineParamIsFlag = true, displayOrder = 4) public boolean getGenerateSaliencyMap() {
		return generateSaliencyMap;
	}

	public void setGenerateSaliencyMap(boolean generateSaliencyMap) {
		this.generateSaliencyMap = generateSaliencyMap;
	}

	@OptionMetadata(displayName = "Saliency map generator", description = "Saliency map options", commandLineParamName = "saliency-map", commandLineParamSynopsis = "-saliency-map <options>", displayOrder = 5) public AbstractCNNSaliencyMapWrapper getSaliencyMapWrapper() {
		return saliencyMapWrapper;
	}

	public void setSaliencyMapWrapper(AbstractCNNSaliencyMapWrapper saliencyMapWrapper) {
		this.saliencyMapWrapper = saliencyMapWrapper;
	}

	/**
	 * Perform any setup stuff that might need to happen before execution.
	 *
	 * @throws Exception if a problem occurs during setup
	 */
	@Override public void preExecution() throws Exception {

	}

	/**
	 * Run this tool from the command line.
	 *
	 * @param toRun   Object to run
	 * @param options Command line options
	 * @throws Exception invalid arguments.
	 */
	private void commandLineRun(Object toRun, String[] options) throws Exception {
		if (!(toRun instanceof Dl4jCNNExplorer)) {
			throw new IllegalArgumentException("Object to execute is not a " + "Dl4jCNNExplorer!");
		}

		Dl4jCNNExplorer explorer = (Dl4jCNNExplorer) toRun;

		// Parse the command line options
		String inputImagePath;
		try {
			inputImagePath = weka.core.Utils.getOption("i", options);
			if (inputImagePath.equals("")) {
				throw new WekaException("Please supply an image file with the -i <image path> arg");
			}
			explorer.setOptions(options);
			// User wants to generate saliency map but hasn't supplied a location to save it to - throw an error
			if (explorer.getGenerateSaliencyMap() && !Utils
					.notDefaultFileLocation(explorer.saliencyMapWrapper.getOutputFile())) {
				throw new WekaException(
						"Please supply output file location in the saliency map generator options e.g.:\n"
								+ "\t-saliency-map \".WekaScoreCAM -bs 8 -output output.png\"");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			printInfo();
			return;
		}

		// Run the explorer
		explorer.init();
		explorer.processImage(new File(inputImagePath));
		if (explorer.generateSaliencyMap)
			explorer.generateAndSaveOutputMap();
		// Output the results to the command line
		System.out.println(explorer.getCurrentPredictions().toSummaryString());
	}

	/**
	 * Execute the supplied object.
	 *
	 * @param toRun   the object to execute
	 * @param options any options to pass to the object
	 * @throws Exception if a problem occurs.
	 */
	@Override public void run(Object toRun, String[] options) throws Exception {
		ClassLoader origLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

			commandLineRun(toRun, options);
		} finally {
			Thread.currentThread().setContextClassLoader(origLoader);
		}
	}

	/**
	 * Print the usage options to standard err.
	 */
	private void printInfo() {
		System.err.println("\nUsage:\n" + "\tDl4jCNNExplorer [options]\n" + "\n" + "Options:\n");

		Enumeration<Option> enm = ((OptionHandler) new Dl4jCNNExplorer()).listOptions();
		while (enm.hasMoreElements()) {
			Option option = enm.nextElement();
			System.err.println(option.synopsis());
			System.err.println(option.description());
		}
	}

	/**
	 * Perform any teardown stuff that might need to happen after execution.
	 *
	 * @throws Exception if a problem occurs during teardown
	 */
	@Override public void postExecution() throws Exception {

	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override public Enumeration<Option> listOptions() {
		return Option.listOptionsForClass(this.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override public String[] getOptions() {
		return Option.getOptionsForHierarchy(this, this.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		Option.setOptionsForHierarchy(options, this, this.getClass());
		weka.core.Utils.checkForRemainingOptions(options);
	}
}
