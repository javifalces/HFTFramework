package weka.dl4j.zoo;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.core.OptionMetadata;
import weka.dl4j.enums.PretrainedType;
import weka.dl4j.zoo.keras.VGG;

/**
 * Wrapper class for Keras version of VGG.
 */
public class KerasVGG extends AbstractZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = 4745962510013669482L;

	/**
	 * Desired version of the model.
	 */
	private VGG.VARIATION variation = VGG.VARIATION.VGG16;

	/**
	 * Instantiate the model.
	 */
	public KerasVGG() {
		setVariation(VGG.VARIATION.VGG16);
		setPretrainedType(PretrainedType.IMAGENET);
		setNumFExtractOutputs(4096);
		setFeatureExtractionLayer("fc2");
		setOutputLayer("predictions");
	}

	@OptionMetadata(description = "The model variation to use.", displayName = "Model Variation", commandLineParamName = "variation", commandLineParamSynopsis = "-variation <String>") public VGG.VARIATION getVariation() {
		return variation;
	}

	@Override public ImagePreProcessingScaler getImagePreprocessingScaler() {
		return null;
	}

	public void setVariation(VGG.VARIATION var) {
		variation = var;
	}

	@Override public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
		VGG vgg = new VGG();
		vgg.setVariation(variation);

		return initZooModel(vgg, null, seed, numLabels, filterMode);
	}

	@Override public int[] getInputShape() {
		return VGG.inputShape;
	}
}
