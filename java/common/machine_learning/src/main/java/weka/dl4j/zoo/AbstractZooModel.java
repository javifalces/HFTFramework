package weka.dl4j.zoo;

import lombok.extern.log4j.Log4j2;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.layers.GlobalPoolingLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import weka.classifiers.functions.Dl4jMlpClassifier;
import weka.core.*;
import weka.core.progress.ProgressManager;
import weka.dl4j.enums.PretrainedType;
import weka.gui.ProgrammaticProperty;

import java.io.Serializable;
import java.util.*;

/**
 * This class contains the logic necessary to load the pretrained weights for a given zoo model
 * <p>
 * It also handles the addition/removal of output layers to enable training the model in DL4J.
 *
 * @author Rhys Compton
 */
@Log4j2 public abstract class AbstractZooModel implements OptionHandler, Serializable {

	/**
	 * Unique ID for version of class.
	 */
	private static final long serialVersionUID = -4598529061609767660L;

	/**
	 * Dataset that the pretrained weights are trained on.
	 */
	protected PretrainedType m_pretrainedType = PretrainedType.NONE;
	/**
	 * Output layer of the model to be taken off (and replace by our custom output layer).
	 */
	protected String m_outputLayer;
	/**
	 * Feature extraction layer of the model (which we'll attach our output layer to) and use for feature extraction.
	 */
	protected String m_featureExtractionLayer;
	/**
	 * Name of the output layer we attach to the end of the model.
	 */
	protected String m_predictionLayerName = "weka_predictions";
	/**
	 * Names of extraneous layers to completely remove (doesn't keep the weights of them).
	 */
	protected String[] m_extraLayersToRemove = new String[0];
	/**
	 * Number of dimensions of the feature extraction layer.
	 */
	protected int m_numFExtractOutputs;
	/**
	 * Random seed.
	 */
	private long seed;
	/**
	 * Number of output labels we want to classify on.
	 */
	private long numLabels;
	/**
	 * Are we loading this model for use as feature extractor (no need to add output layer).
	 */
	protected boolean filterMode;
	/**
	 * Do the activations from featureLayer require pooling (too high dimensionality).
	 */
	protected boolean requiresPooling = false;
	/**
	 * Is the model in channels-last order? (i.e. data must come in [minibatch, height, width, channels]
	 * instead of the default [minibatch, channels, height, width]
	 */
	protected boolean channelsLast = false;

	/**
	 * Initialize the ZooModel as MLP.
	 *
	 * @param numLabels  Number of labels to adjust the output
	 * @param seed       Seed
	 * @param shape      shape
	 * @param filterMode True if creating for feature extraction
	 * @return MultiLayerNetwork of the specified ZooModel
	 * @throws UnsupportedOperationException Init(...) was not supported (only CustomNet)
	 */
	public abstract ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode)
			throws UnsupportedOperationException;

	/**
	 * Convenience method to returns a default pretrained graph for this zoo model
	 *
	 * @return
	 */
	public ComputationGraph getDefaultGraph() {
		return this.init(2, 1, this.getInputShape(), true);
	}

	/**
	 * Get the input shape of this zoomodel
	 *
	 * @return Input shape of this zoomodel
	 */
	public abstract int[] getInputShape();

	public String getPrettyName() {
		Enum variation = getVariation();
		if (variation == null) {
			return this.getClass().getSimpleName();
		} else {
			return this.getClass().getSimpleName() + " (" + variation + ")";
		}
	}

	/**
	 * Get the current variation of the zoo model (e.g., Resnet50 or Resnet101)
	 *
	 * @return Variation
	 */
	public abstract Enum getVariation();

	/**
	 * Does the model require input images to be preprocessed?.
	 *
	 * @return true if the model input should be rescaled
	 */
	public boolean requiresPreProcessing() {
		return getImagePreprocessingScaler() != null;
	}

	/**
	 * Get the preprocessor to process this model's data with
	 *
	 * @return DataSetPreprocessor
	 */
	public abstract ImagePreProcessingScaler getImagePreprocessingScaler();

	@OptionMetadata(displayName = "Image channels last", description = "Set to true to supply image channels last. "
			+ "The default value will usually be correct, so as an end user you shouldn't need to change this setting. "
			+ "If you do be aware that it may break the model.", commandLineParamName = "channelsLast", commandLineParamSynopsis = "-channelsLast <boolean>") public boolean getChannelsLast() {
		return channelsLast;
	}

	public void setChannelsLast(boolean channelsLast) {
		this.channelsLast = channelsLast;
	}

	@ProgrammaticProperty public boolean isRequiresPooling() {
		return requiresPooling;
	}

	public void setRequiresPooling(boolean requiresPooling) {
		this.requiresPooling = requiresPooling;
	}

	/**
	 * Initialize the zoo model with the supplied params.
	 *
	 * @param zooModel   Zoo model family to use
	 * @param defaultNet Default ComputationGraph to use if loading weights fails
	 * @param seed       Random seed to initialize with
	 * @param numLabels  Number of output labels
	 * @param filterMode True if using this zoo model for a filter - output layers don't need to be setup
	 * @return ComputationGraph - if all succeeds then will be initialized with pretrained weights
	 */
	public ComputationGraph initZooModel(org.deeplearning4j.zoo.ZooModel zooModel, ComputationGraph defaultNet,
			long seed, int numLabels, boolean filterMode) {

		this.seed = seed;
		this.numLabels = numLabels;
		this.filterMode = filterMode;

		// If no pretrained weights specified, simply return the standard model
		if (m_pretrainedType == PretrainedType.NONE)
			return finish(defaultNet);

		// If the specified pretrained weights aren't available, return the standard model
		if (!checkPretrained(zooModel)) {
			return null;
		}

		ComputationGraph pretrainedModel = null;
		// Attempt to download the model weights
		ProgressManager progressManager = new ProgressManager(
				"Initializing pretrained model (may require downloading weights)...");
		progressManager.start();
		try {
			pretrainedModel = downloadWeights(zooModel);
		} catch (ThreadDeath threadDeath) {
			log.error("Downloading weights stopped prematurely...");
			progressManager.finish();
			throw new ThreadDeath();
		}

		progressManager.finish();
		// If downloading the weights fails, return the standard model
		if (pretrainedModel == null)
			return finish(defaultNet);

		// If all has gone well, we have the pretrained weights
		return finish(pretrainedModel);
	}

	/**
	 * Final endpoint for ComputationGraph before returning.
	 *
	 * @param computationGraph Input ComputationGraph
	 * @return Finalized ComputationGraph
	 */
	private ComputationGraph finish(ComputationGraph computationGraph) {
		log.debug(computationGraph.summary());
		return addFinalOutputLayer(computationGraph);
	}

	/**
	 * Checks if we need to add a final output layer - also applies pooling beforehand if necessary.
	 *
	 * @param computationGraph Input ComputationGraph
	 * @return Finalized ComputationGraph
	 */
	protected ComputationGraph addFinalOutputLayer(ComputationGraph computationGraph) {
		org.deeplearning4j.nn.conf.layers.Layer lastLayer = computationGraph.getLayers()[computationGraph.getNumLayers()
				- 1].conf().getLayer();
		if (Dl4jMlpClassifier.isValidOutputLayer(filterMode, lastLayer)) {
			log.debug("No need to add output layer, ignoring");
			return computationGraph;
		}
		try {
			TransferLearning.GraphBuilder graphBuilder;

			if (requiresPooling)
				graphBuilder = new TransferLearning.GraphBuilder(computationGraph)
						.fineTuneConfiguration(getFineTuneConfig())
						.addLayer("intermediate_pooling", new GlobalPoolingLayer.Builder().build(),
								m_featureExtractionLayer)
						.addLayer(m_predictionLayerName, createOutputLayer(), "intermediate_pooling")
						.setOutputs(m_predictionLayerName);
			else
				graphBuilder = new TransferLearning.GraphBuilder(computationGraph)
						.fineTuneConfiguration(getFineTuneConfig())
						.addLayer(m_predictionLayerName, createOutputLayer(), m_featureExtractionLayer)
						.setOutputs(m_predictionLayerName);

			// Remove the old output layer, but keep the connections
			graphBuilder.removeVertexKeepConnections(m_outputLayer);
			// Remove any other layers we don't want
			for (String layer : m_extraLayersToRemove) {
				graphBuilder.removeVertexAndConnections(layer);
			}

			log.debug("Finished adding output layer");
			return graphBuilder.build();
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error(computationGraph.summary());
			return computationGraph;
		}

	}

	/**
	 * @return True if current model is pretrained
	 */
	public boolean isPretrained() {
		return m_pretrainedType != PretrainedType.NONE;
	}

	/**
	 * We need to create and set the fine tuning config
	 *
	 * @return Default fine tuning config
	 */
	protected FineTuneConfiguration getFineTuneConfig() {
		return new FineTuneConfiguration.Builder().optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
				.updater(new Nesterovs(5e-5)).seed(seed).build();
	}

	/**
	 * Attempt to download weights for the given model with our pretrained type.
	 *
	 * @param zooModel Zoo model to download weights for.
	 * @return Model with initialized weights.
	 * @throws Exception If error occurs during download of weights.
	 */
	private ComputationGraph attemptToDownloadWeights(org.deeplearning4j.zoo.ZooModel zooModel) throws Exception {
		Object pretrained = zooModel.initPretrained(m_pretrainedType.getBackend());
		if (pretrained == null) {
			throw new Exception("Error while initialising model");
		}
		if (pretrained instanceof MultiLayerNetwork) {
			return ((MultiLayerNetwork) pretrained).toComputationGraph();
		} else {
			return (ComputationGraph) pretrained;
		}
	}

	/**
	 * Attempts to download weights for the given zoo model, repeating numTries times.
	 *
	 * @param zooModel Model to try download weights for
	 * @return new ComputationGraph initialized with the given PretrainedType
	 */
	protected ComputationGraph downloadWeights(org.deeplearning4j.zoo.ZooModel zooModel) {
		int numTries = 3;
		for (int i = 0; i < numTries; i++) {
			try {
				log.info(String.format("Attempt %d to download weights", (i + 1)));
				return attemptToDownloadWeights(zooModel);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * We need a layer with the correct number of outputs.
	 *
	 * @return Default output layer
	 */
	protected OutputLayer createOutputLayer() {
		return new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).nIn(m_numFExtractOutputs)
				.nOut(numLabels).weightInit(new NormalDistribution(0,
						0.2 * (2.0 / (4096 + numLabels)))) //This weight init dist gave better results than Xavier
				.activation(Activation.SOFTMAX).build();
	}

	/**
	 * Checks if the zoo model has the specific pretrained type available.
	 *
	 * @param dl4jModelType ZooModel to check
	 * @return True if model supports `m_pretrainedType` weights
	 */
	protected boolean checkPretrained(org.deeplearning4j.zoo.ZooModel dl4jModelType) {
		Set<PretrainedType> availableTypes = getAvailablePretrainedWeights(dl4jModelType);
		if (availableTypes.isEmpty()) {
			log.error("Sorry, no pretrained weights are available for this model, "
					+ "please explicitly set pretrained type to NONE");
			return false;
		}
		if (!availableTypes.contains(m_pretrainedType) && m_pretrainedType != PretrainedType.NONE) {
			log.error(String.format("%s weights are not available for this model, " + "please try one of: %s",
					m_pretrainedType, availableTypes.toString()));
			return false;
		}
		return true;
	}

	/**
	 * Get all Pretrained types this ZooModel supports.
	 *
	 * @param zooModel ZooModel to check
	 * @return Set of pretrained types the model supports
	 */
	private Set<PretrainedType> getAvailablePretrainedWeights(org.deeplearning4j.zoo.ZooModel zooModel) {
		Set<PretrainedType> availableTypes = new HashSet<>();
		for (PretrainedType pretrainedType : PretrainedType.values()) {
			if (pretrainedType == PretrainedType.NONE)
				continue;

			if (zooModel.pretrainedAvailable(pretrainedType.getBackend())) {
				availableTypes.add(pretrainedType);
			}
		}
		return availableTypes;
	}

	@OptionMetadata(description = "Pretrained Type (IMAGENET, VGGFACE, MNIST)", displayName = "Pretrained Type", commandLineParamName = "pretrained", commandLineParamSynopsis = "-pretrained <string>") public PretrainedType getPretrainedType() {
		return m_pretrainedType;
	}

	public void setPretrainedType(PretrainedType pretrainedType) {
		this.m_pretrainedType = pretrainedType;
	}

	@ProgrammaticProperty public String getOutputlayer() {
		return m_outputLayer;
	}

	public void setOutputLayer(String m_outputLayer) {
		this.m_outputLayer = m_outputLayer;
	}

	@ProgrammaticProperty public String getFeatureExtractionLayer() {
		return m_featureExtractionLayer;
	}

	public void setFeatureExtractionLayer(String m_featureExtractionLayer) {
		this.m_featureExtractionLayer = m_featureExtractionLayer;
	}

	@ProgrammaticProperty public String[] getExtraLayersToRemove() {
		return m_extraLayersToRemove;
	}

	public void setExtraLayersToRemove(String[] m_extraLayersToRemove) {
		this.m_extraLayersToRemove = m_extraLayersToRemove;
	}

	@ProgrammaticProperty public int getNumFExtractOutputs() {
		return m_numFExtractOutputs;
	}

	public void setNumFExtractOutputs(int m_numFExtractOutputs) {
		this.m_numFExtractOutputs = m_numFExtractOutputs;
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

		Utils.checkForRemainingOptions(options);
	}
}
