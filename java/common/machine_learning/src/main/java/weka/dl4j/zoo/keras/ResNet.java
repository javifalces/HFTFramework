package weka.dl4j.zoo.keras;

/**
 * Wrapper class for the different versions of ResNet. Has the same interface as Dl4j zoo models, so we can
 * simply call initPretrained().
 */
public class ResNet extends KerasZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = 5315928260273073018L;

	/**
	 * Different variations of the model.
	 */
	public enum VARIATION {
		/**
		 * ResNet50 version.
		 */
		RESNET50,
		/**
		 * ResNet50v2 version.
		 */
		RESNET50V2,
		/**
		 * ResNet101 version.
		 */
		RESNET101,
		/**
		 * ResNet101v2 version.
		 */
		RESNET101V2,
		/**
		 * ResNet152 version.
		 */
		RESNET152,
		/**
		 * ResNet152v2 version.
		 */
		RESNET152V2
	}

	;

	/**
	 * Default input shape of the model.
	 */
	public static int[] inputShape = new int[] { 3, 224, 224 };

	/**
	 * Default variation of the model.
	 */
	protected VARIATION m_variation = VARIATION.RESNET50;

	/**
	 * Instantiate the model.
	 */
	public ResNet() {
		setVariation(VARIATION.RESNET50);
	}

	@Override public void setVariation(Enum variation) {
		this.m_variation = (VARIATION) variation;
	}

	@Override public String modelFamily() {
		return "keras_resnet";
	}

	@Override public String modelPrettyName() {
		switch (m_variation) {
			case RESNET50:
				return "KerasResNet50";
			case RESNET50V2:
				return "KerasResNet50V2";
			case RESNET101:
				return "KerasResNet101";
			case RESNET101V2:
				return "KerasResNet101V2";
			case RESNET152:
				return "KerasResNet152";
			case RESNET152V2:
				return "KerasResNet152V2";
			default:
				return null;
		}
	}
}
