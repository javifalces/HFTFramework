package weka.dl4j.interpretability;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import weka.classifiers.functions.Dl4jMlpClassifier;
import weka.dl4j.Utils;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.core.progress.ProgressManager;
import weka.dl4j.inference.CustomModelSetup;
import weka.dl4j.zoo.AbstractZooModel;
import weka.gui.ProgrammaticProperty;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * WEKA Wrapper for a Saliency Map Generator (e.g., ScoreCAM).
 * Provides the WEKA handles and getters/setters to control the
 * generator from the command line/GUI.
 */
@Log4j2 public abstract class AbstractCNNSaliencyMapWrapper implements Serializable, OptionHandler {

	/**
	 * Displays progress of the current process (feature extraction, training, etc.)
	 */
	protected ProgressManager progressManager;

	/**
	 * Batch size for generating the saliency map.
	 */
	protected int batchSize = 1;

	/**
	 * IDs of the classes we want to generate heatmaps for.
	 */
	protected int[] targetClassIDs = new int[] { -1 };

	/**
	 * File to save heatmap image to.
	 */
	protected File outputFile = new File(Utils.defaultFileLocation());

	/**
	 * Should we normalize the heatmap.
	 */
	protected boolean normalizeHeatmap = true;

	/**
	 * Classmap to match with target classes.
	 */
	protected String[] classMap;

	/**
	 * Model used for feature extraction.
	 */
	protected Dl4jMlpClassifier dl4jMlpClassifier;

	/**
	 * Custom model settings, to be used when running on a custom-trained model.
	 */
	protected CustomModelSetup customModelSetup = new CustomModelSetup();

	/**
	 * Main processing entrypoint. Takes an image as input, and performs the necessary
	 * processing on it. Note that this is performed separately to the actual saliency map
	 * generation. This method should only perform the expensive one-time processing on an image.
	 *
	 * @param imageFile Image to process heatmap for.
	 */
	public abstract void processImage(File imageFile);

	/**
	 * Generates heatmaps, returning a human-viewable heatmap summary.
	 *
	 * @return Human-viewable heatmap summary.
	 */
	public abstract BufferedImage generateHeatmapToImage();

	/**
	 * Save the supplied composite image to a file.
	 *
	 * @param completeCompositeImage Image to save.
	 */
	public void saveResult(BufferedImage completeCompositeImage) {
		if (Utils.notDefaultFileLocation(getOutputFile())) {
			log.info(String.format("Output file location = %s", getOutputFile()));
			try {
				ImageIO.write(completeCompositeImage, "png", getOutputFile());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			log.error("No output file location given - not saving saliency map");
		}
	}

	@ProgrammaticProperty public String[] getClassMap() {
		return classMap;
	}

	public void setClassMap(String[] classMap) {
		this.classMap = classMap;
	}

	@ProgrammaticProperty public Dl4jMlpClassifier getDl4jMlpClassifier() {
		return dl4jMlpClassifier;
	}

	public void setDl4jMlpClassifier(Dl4jMlpClassifier dl4jMlpClassifier) {
		this.dl4jMlpClassifier = dl4jMlpClassifier;
	}

	@ProgrammaticProperty public CustomModelSetup getCustomModelSetup() {
		return customModelSetup;
	}

	public void setCustomModelSetup(CustomModelSetup customModelSetup) {
		this.customModelSetup = customModelSetup;
	}

	@OptionMetadata(displayName = "Batch size", description = "The mini batch size to use for map generation", commandLineParamName = "bs", commandLineParamSynopsis = "-bs <int>", displayOrder = 1) public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	@OptionMetadata(displayName = "Target Classes", description =
			"Output class to generate saliency maps for; default is -1 (use the highest probability class). "
					+ "This only needs to be set if wanting to use a non-default class from the *command line*; if using the *GUI*, "
					+ "the 'View Saliency Map' window contains the interface for setting this.", commandLineParamName = "target-classes", commandLineParamSynopsis = "-target-classes <int,int,...>", displayOrder = 2) @ProgrammaticProperty public String getTargetClassIDs() {
		return StringUtils.join(ArrayUtils.toObject(targetClassIDs), ",");
	}

	public void setTargetClassIDs(String targetClassIDs) {
		this.targetClassIDs = Arrays.stream(targetClassIDs.split(",")).mapToInt(Integer::parseInt).toArray();
	}

	public int[] getTargetClassIDsAsInt() {
		return targetClassIDs;
	}

	public void setTargetClassIDsAsInt(int[] targetClassIDs) {
		this.targetClassIDs = targetClassIDs;
	}

	@OptionMetadata(displayName = "Output file location", description = "File for the saliency map to be saved in", commandLineParamName = "output", commandLineParamSynopsis = "-output <file location>", displayOrder = 3) @ProgrammaticProperty public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFileLocation) {
		this.outputFile = outputFileLocation;
	}

	@OptionMetadata(displayName = "Normalize heatmap", description = "When generating the heatmap, should the values be normalized to be in [0, 1]", commandLineParamName = "normalize", commandLineParamSynopsis = "-normalize", commandLineParamIsFlag = true) @ProgrammaticProperty public boolean getNormalizeHeatmap() {
		return normalizeHeatmap;
	}

	public void setNormalizeHeatmap(boolean normalizeHeatmap) {
		this.normalizeHeatmap = normalizeHeatmap;
	}

	public ProgressManager getProgressManager() {
		return progressManager;
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration<Option> listOptions() {
		return Option.listOptionsForClassHierarchy(this.getClass(), AbstractZooModel.class).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {
		return Option.getOptionsForHierarchy(this, AbstractZooModel.class);
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		Option.setOptionsForHierarchy(options, this, AbstractZooModel.class);

		weka.core.Utils.checkForRemainingOptions(options);
	}

}
