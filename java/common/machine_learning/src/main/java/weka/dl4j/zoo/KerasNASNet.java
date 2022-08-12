package weka.dl4j.zoo;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.core.OptionMetadata;
import weka.dl4j.enums.PretrainedType;
import weka.dl4j.zoo.keras.NASNet;

/**
 * Wrapper class for Keras version of NASNet.
 */
public class KerasNASNet extends AbstractZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = -6046846303686819108L;

	/**
	 * Desired version of the model.
	 */
	private NASNet.VARIATION variation = NASNet.VARIATION.MOBILE;

	/**
	 * Instantiate the model.
	 */
	public KerasNASNet() {
		setVariation(NASNet.VARIATION.MOBILE);
		setPretrainedType(PretrainedType.IMAGENET);
		setFeatureExtractionLayer("global_average_pooling2d_1");
		setOutputLayer("predictions");
	}

	@OptionMetadata(description = "The model variation to use.", displayName = "Model Variation", commandLineParamName = "variation", commandLineParamSynopsis = "-variation <String>") public NASNet.VARIATION getVariation() {
		return variation;
	}

	@Override public ImagePreProcessingScaler getImagePreprocessingScaler() {
		return new ImagePreProcessingScaler(-1, 1);
	}

	public void setVariation(NASNet.VARIATION var) {
		variation = var;
		int numFExtractOutputs = -1;
		switch (variation) {
			case MOBILE:
				numFExtractOutputs = 1056;
				break;
			case LARGE:
				numFExtractOutputs = 4032;
				break;
		}
		setNumFExtractOutputs(numFExtractOutputs);
	}

	@Override public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
		NASNet nasNet = new NASNet();
		nasNet.setVariation(variation);

		return initZooModel(nasNet, null, seed, numLabels, filterMode);
	}

	@Override public int[] getInputShape() {
		return NASNet.inputShape;
	}
}
