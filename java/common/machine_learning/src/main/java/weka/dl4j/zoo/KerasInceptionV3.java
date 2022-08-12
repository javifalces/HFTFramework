package weka.dl4j.zoo;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.core.OptionMetadata;
import weka.dl4j.enums.PretrainedType;
import weka.dl4j.zoo.keras.InceptionV3;

/**
 * Wrapper class for Keras version of InceptionV3.
 */
public class KerasInceptionV3 extends AbstractZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = 4695067559456124034L;

	/**
	 * Desired version of the model.
	 */
	private InceptionV3.VARIATION variation = InceptionV3.VARIATION.STANDARD;

	/**
	 * Instantiate the model.
	 */
	public KerasInceptionV3() {
		setVariation(InceptionV3.VARIATION.STANDARD);
		setPretrainedType(PretrainedType.IMAGENET);
		setNumFExtractOutputs(2048);
		setFeatureExtractionLayer("avg_pool");
		setOutputLayer("predictions");
	}

	@OptionMetadata(description = "The model variation to use.", displayName = "Model Variation", commandLineParamName = "variation", commandLineParamSynopsis = "-variation <String>") public InceptionV3.VARIATION getVariation() {
		return variation;
	}

	@Override public ImagePreProcessingScaler getImagePreprocessingScaler() {
		return new ImagePreProcessingScaler(-1, 1);
	}

	public void setVariation(InceptionV3.VARIATION var) {
		variation = var;
	}

	@Override public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
		InceptionV3 inceptionV3 = new InceptionV3();
		inceptionV3.setVariation(variation);

		return initZooModel(inceptionV3, null, seed, numLabels, filterMode);
	}

	@Override public int[] getInputShape() {
		return InceptionV3.inputShape;
	}
}
