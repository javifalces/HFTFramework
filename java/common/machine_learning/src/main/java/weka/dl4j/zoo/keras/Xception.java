package weka.dl4j.zoo.keras;

/**
 * Wrapper class for the different versions of Xception. Has the same interface as Dl4j zoo models, so we can
 * simply call initPretrained().
 */
public class Xception extends KerasZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = -5423178061075685025L;

	/**
	 * Different variations of the model.
	 */
	public enum VARIATION {
		/**
		 * Standard version of the model.
		 */
		STANDARD
	}

	;

	/**
	 * Default input shape of the model.
	 */
	public static int[] inputShape = new int[] { 3, 229, 229 };

	/**
	 * Default variation of the model.
	 */
	protected VARIATION m_variation = VARIATION.STANDARD;

	/**
	 * Instantiate the model.
	 */
	public Xception() {
		setVariation(VARIATION.STANDARD);
	}

	@Override public void setVariation(Enum variation) {
		this.m_variation = (VARIATION) variation;
	}

	@Override public String modelFamily() {
		return "keras_xception";
	}

	@Override public String modelPrettyName() {
		switch (m_variation) {
			case STANDARD:
				return "KerasXception";
			default:
				return null;
		}
	}
}
