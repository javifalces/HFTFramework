package weka.dl4j.zoo.keras;

/**
 * Wrapper class for the different versions of MobileNet. Has the same interface as Dl4j zoo models, so we can
 * simply call initPretrained().
 */
public class MobileNet extends KerasZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = -5326817943250936967L;

	/**
	 * Different variations of the model.
	 */
	public enum VARIATION {
		/**
		 * V1 of MobileNet.
		 */
		V1,
		/**
		 * V2 of MobileNet.
		 */
		V2
	}

	;

	/**
	 * Default input shape of the model.
	 */
	public static int[] inputShape = new int[] { 3, 224, 224 };

	/**
	 * Default variation of the model.
	 */
	protected VARIATION m_variation = VARIATION.V2;

	/**
	 * Instantiate the model.
	 */
	public MobileNet() {
		setVariation(VARIATION.V2);
	}

	@Override public void setVariation(Enum variation) {
		this.m_variation = (VARIATION) variation;
	}

	@Override public String modelFamily() {
		return "keras_mobilenet";
	}

	@Override public String modelPrettyName() {
		switch (m_variation) {
			case V1:
				return "MobileNet";
			case V2:
				return "MobileNetV2";
			default:
				return null;
		}
	}
}
