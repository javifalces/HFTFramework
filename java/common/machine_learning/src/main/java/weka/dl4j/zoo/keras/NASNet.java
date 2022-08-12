package weka.dl4j.zoo.keras;

/**
 * Wrapper class for the different versions of NASNet. Has the same interface as Dl4j zoo models, so we can
 * simply call initPretrained().
 */
public class NASNet extends KerasZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = -8106239516237343141L;

	/**
	 * Different variations of the model.
	 */
	public enum VARIATION {
		/**
		 * Mobile variation of NASNet.
		 */
		MOBILE,
		/**
		 * Large variation of NASNet.
		 */
		LARGE
	}

	;

	/**
	 * Default input shape of the model.
	 */
	public static int[] inputShape = new int[] { 3, 224, 224 };

	/**
	 * Default variation of the model.
	 */
	protected VARIATION m_variation = VARIATION.MOBILE;

	/**
	 * Instantiate the model.
	 */
	public NASNet() {
		setVariation(VARIATION.MOBILE);
	}

	@Override public void setVariation(Enum variation) {
		this.m_variation = (VARIATION) variation;

		if (this.m_variation == VARIATION.MOBILE) {
			inputShape[1] = 224;
			inputShape[2] = 224;
		} else if (this.m_variation == VARIATION.LARGE) {
			inputShape[1] = 331;
			inputShape[2] = 331;
		}
	}

	@Override public String modelFamily() {
		return "keras_nasnet";
	}

	@Override public String modelPrettyName() {
		switch (m_variation) {
			case MOBILE:
				return "KerasNASNetMobile";
			case LARGE:
				return "KerasNASNetLarge";
			default:
				return null;
		}
	}
}
