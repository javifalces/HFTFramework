package weka.dl4j.zoo;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.core.OptionMetadata;
import weka.dl4j.enums.PretrainedType;
import weka.dl4j.zoo.keras.ResNet;

/**
 * Wrapper class for Keras version of ResNet.
 */
public class KerasResNet extends AbstractZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = 5525252856492208127L;

	/**
	 * Desired version of the model.
	 */
	private ResNet.VARIATION variation = ResNet.VARIATION.RESNET50;

	/**
	 * Instantiate the model.
	 */
	public KerasResNet() {
		setVariation(ResNet.VARIATION.RESNET50);
		setPretrainedType(PretrainedType.IMAGENET);
		setNumFExtractOutputs(2048);
		setFeatureExtractionLayer("avg_pool");
		setOutputLayer("probs");
	}

	@OptionMetadata(description = "The model variation to use.", displayName = "Model Variation", commandLineParamName = "variation", commandLineParamSynopsis = "-variation <String>") public ResNet.VARIATION getVariation() {
		return variation;
	}

	@Override public ImagePreProcessingScaler getImagePreprocessingScaler() {
		ResNet.VARIATION variation = getVariation();

		if (variation == ResNet.VARIATION.RESNET50V2 || variation == ResNet.VARIATION.RESNET101V2
				|| variation == ResNet.VARIATION.RESNET152V2)
			return new ImagePreProcessingScaler(-1, 1);
		else
			return null;
	}

	public void setVariation(ResNet.VARIATION var) {
		variation = var;
	}

	@Override public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
		ResNet resNet = new ResNet();
		resNet.setVariation(variation);

		return initZooModel(resNet, null, seed, numLabels, filterMode);
	}

	@Override public int[] getInputShape() {
		return ResNet.inputShape;
	}
}
