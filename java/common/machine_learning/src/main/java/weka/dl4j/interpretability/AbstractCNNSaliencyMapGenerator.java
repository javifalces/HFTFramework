package weka.dl4j.interpretability;

import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.dl4j.inference.Prediction;
import weka.dl4j.inference.PredictionClass;
import weka.dl4j.interpretability.listeners.IterationIncrementListener;
import weka.dl4j.interpretability.listeners.IterationsFinishedListener;
import weka.dl4j.interpretability.listeners.IterationsStartedListener;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic class for a saliency map generator.
 * Only implemented by ScoreCAM currently so some fields may need to be refactored when implementing a new
 * generator method. This method should be kept as free of Weka classes as possible, so that this can all be
 * refactored into the DL4J package (which doesn't have WEKA as a dependency).
 */
public abstract class AbstractCNNSaliencyMapGenerator {

	/**
	 * Model used to generate the saliency map.
	 */
	protected ComputationGraph computationGraph = null;

	/**
	 * Batch size for generating the saliency map.
	 */
	protected int batchSize = 1;

	/**
	 * Used to preprocess the image before passing it to the model. Must be the same as what the model was trained with.
	 */
	protected ImagePreProcessingScaler imagePreProcessingScaler = null;

	/**
	 * Flag for flipping the image channels. Must be the same as what the model was trained with.
	 */
	protected boolean imageChannelsLast = false;

	/**
	 * Input shape for the image. Must be the same as what the model was trained with.
	 */
	protected InputType.InputTypeConvolutional modelInputShape;

	/**
	 * Human-readable name for the model. Printed on the saliency map output.
	 */
	protected String modelName;

	/**
	 * Human-readable name for the image passed in. Printed on the saliency map output.
	 */
	protected String inputFilename;

	/**
	 * How much padding to add around the images when generating the output image.
	 */
	protected int outsideMargin = 25;

	/**
	 * How much padding to add in between the images when generating the output image.
	 */
	protected int insidePadding = 20;

	/**
	 * Font size for printing on the image.
	 */
	protected int fontSpacing = 12;

	/**
	 * Main processing entrypoint. Takes an image as input, and performs the necessary
	 * processing on it. Note that this is performed separately to the actual saliency map
	 * generation. This method should only perform the expensive one-time processing on an image.
	 *
	 * @param imageFile File to process into saliency map.
	 */
	public abstract void processImage(File imageFile);

	/**
	 * Generates heatmaps for the supplied classes, returning a human-viewable heatmap summary.
	 *
	 * @param targetClasses Classes to generate saliency maps for.
	 * @param classMap      Appropriate class map for the supplied classes.
	 * @param normalize     Should we normalize the heatmap?
	 * @return Human-viewable heatmap summary.
	 */
	public abstract BufferedImage generateHeatmapToImage(int[] targetClasses, String[] classMap, boolean normalize);

	/**
	 * Event listener for when the heatmap generation iterations start.
	 */
	protected List<IterationsStartedListener> iterationsStartedListeners = new ArrayList<>();

	/**
	 * Event listener for when the heatmap generation iterations increment.
	 */
	protected List<IterationIncrementListener> iterationIncrementListeners = new ArrayList<>();

	/**
	 * Event listener for when the heatmap generation iterations finish.
	 */
	protected List<IterationsFinishedListener> iterationsFinishedListeners = new ArrayList<>();

	/**
	 * Add an event listener to the iterations started event.
	 *
	 * @param listener Listener to call when iterations start.
	 */
	public void addIterationsStartedListener(IterationsStartedListener listener) {
		iterationsStartedListeners.add(listener);
	}

	/**
	 * Add an event listener to the iterations increment event.
	 *
	 * @param listener Listener to call when iterations increment.
	 */
	public void addIterationIncrementListener(IterationIncrementListener listener) {
		iterationIncrementListeners.add(listener);
	}

	/**
	 * Add an event listener to the iterations finished event.
	 *
	 * @param listener Listener to call when iterations finish.
	 */
	public void addIterationsFinishedListeners(IterationsFinishedListener listener) {
		iterationsFinishedListeners.add(listener);
	}

	/* Getters and setters */

	public ComputationGraph getComputationGraph() {
		return computationGraph;
	}

	public void setComputationGraph(ComputationGraph computationGraph) {
		this.computationGraph = computationGraph;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public ImagePreProcessingScaler getImagePreProcessingScaler() {
		return imagePreProcessingScaler;
	}

	public void setImagePreProcessingScaler(ImagePreProcessingScaler imagePreProcessingScaler) {
		this.imagePreProcessingScaler = imagePreProcessingScaler;
	}

	public boolean isImageChannelsLast() {
		return imageChannelsLast;
	}

	public void setImageChannelsLast(boolean imageChannelsLast) {
		this.imageChannelsLast = imageChannelsLast;
	}

	public InputType.InputTypeConvolutional getModelInputShape() {
		return modelInputShape;
	}

	public void setModelInputShape(InputType.InputTypeConvolutional modelInputShape) {
		this.modelInputShape = modelInputShape;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getInputFilename() {
		return inputFilename;
	}

	public void setInputFilename(String inputFilename) {
		this.inputFilename = inputFilename;
	}
}
