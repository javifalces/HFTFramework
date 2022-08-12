package weka.dl4j.zoo;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.core.OptionMetadata;
import weka.dl4j.enums.PretrainedType;
import weka.dl4j.zoo.keras.Xception;

/**
 * Wrapper class for Keras version of Xception.
 */
public class KerasXception extends AbstractZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = -6899732453136761839L;

	/**
	 * Desired version of the model.
	 */
	private Xception.VARIATION variation = Xception.VARIATION.STANDARD;

	/**
	 * Instantiate the model.
	 */
	public KerasXception() {
		setVariation(Xception.VARIATION.STANDARD);
		setPretrainedType(PretrainedType.IMAGENET);
		setNumFExtractOutputs(2048);
		setFeatureExtractionLayer("avg_pool");
		setOutputLayer("predictions");
	}

	@OptionMetadata(description = "The model variation to use.", displayName = "Model Variation", commandLineParamName = "variation", commandLineParamSynopsis = "-variation <String>") public Xception.VARIATION getVariation() {
		return variation;
	}

	@Override public ImagePreProcessingScaler getImagePreprocessingScaler() {
		return new ImagePreProcessingScaler(-1, 1);
	}

	public void setVariation(Xception.VARIATION var) {
		variation = var;
	}

	@Override public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
		Xception xception = new Xception();
		xception.setVariation(variation);

		return initZooModel(xception, null, seed, numLabels, filterMode);
	}

	@Override public int[] getInputShape() {
		return Xception.inputShape;
	}
}
