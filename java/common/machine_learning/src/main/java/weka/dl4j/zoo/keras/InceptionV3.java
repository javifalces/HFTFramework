package weka.dl4j.zoo.keras;

/**
 * Wrapper class for the different versions of InceptionV3. Has the same interface as Dl4j zoo models, so we can
 * simply call initPretrained().
 */
public class InceptionV3 extends KerasZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = -3697352145124549273L;

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
	public static int[] inputShape = new int[] { 3, 299, 299 };

	/**
	 * Default variation of the model.
	 */
	protected VARIATION m_variation = VARIATION.STANDARD;

	/**
	 * Instantiate the model.
	 */
	public InceptionV3() {
		setVariation(VARIATION.STANDARD);
	}

	@Override public void setVariation(Enum variation) {
		this.m_variation = (VARIATION) variation;
	}

	@Override public String modelFamily() {
		return "keras_inceptionv3";
	}

	@Override public String modelPrettyName() {
		return "KerasInceptionV3";
	}
}
