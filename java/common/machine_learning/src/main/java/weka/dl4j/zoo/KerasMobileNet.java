package weka.dl4j.zoo;

public class KerasMobileNet {
	// Uses RELU layer instead of RELU activation layer, which isn't currently supported in DL4J

	//public class KerasMobileNet extends AbstractZooModel {
	//    private static final long serialVersionUID = 8206193669750977225L;
	//
	//    private MobileNet.VARIATION variation = MobileNet.VARIATION.V2;
	//
	//    public KerasMobileNet() {
	//        setVariation(MobileNet.VARIATION.V2);
	//        setPretrainedType(PretrainedType.IMAGENET);
	//    }
	//
	//    @OptionMetadata(
	//            description = "The model variation to use.",
	//            displayName = "Model Variation",
	//            commandLineParamName = "variation",
	//            commandLineParamSynopsis = "-variation <String>"
	//    )
	//    public MobileNet.VARIATION getVariation() {
	//        return variation;
	//    }
	//
	//    public void setVariation(MobileNet.VARIATION var) {
	//        variation = var;
	//        // We may need to update the pretrained values based on the new variation
	//        setPretrainedType(m_pretrainedType);
	//    }
	//
	//    @Override
	//    public void setPretrainedType(PretrainedType pretrainedType) {
	//        switch (variation) {
	//            case V1:
	//                setPretrainedType(pretrainedType, 1000, "reshape_2", "act_softmax");
	//                break;
	//            case V2:
	//                setPretrainedType(pretrainedType, 1280, "global_average_pooling2d_1", "Logits");
	//                break;
	//        }
	//    }
	//
	//    @Override
	//    public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
	//        MobileNet mobileNet = new MobileNet();
	//        mobileNet.setVariation(variation);
	//
	//        return attemptToLoadWeights(mobileNet, null, seed, numLabels, filterMode);
	//    }
	//
	//    @Override
	//    public int[][] getShape() {
	//        int[][] shape = new int[1][];
	//        shape[0] = MobileNet.inputShape;
	//        return shape;
	//    }
}
