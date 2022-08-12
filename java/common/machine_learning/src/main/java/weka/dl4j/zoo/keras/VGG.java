package weka.dl4j.zoo.keras;

/**
 * Wrapper class for the different versions of VGG. Has the same interface as Dl4j zoo models, so we can
 * simply call initPretrained().
 */
public class VGG extends KerasZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = 3908733587819287902L;

	/**
	 * Different variations of the model.
	 */
	public enum VARIATION {
		/**
		 * VGG16 variation.
		 */
		VGG16,
		/**
		 * VGG19 variation.
		 */
		VGG19
	}

	;

	/**
	 * Default input shape of the model.
	 */
	public static int[] inputShape = new int[] { 3, 224, 224 };

	/**
	 * Default variation of the model.
	 */
	protected VARIATION m_variation = VARIATION.VGG16;

	/**
	 * Instantiate the model.
	 */
	public VGG() {
		setVariation(VARIATION.VGG16);
	}

	@Override public void setVariation(Enum variation) {
		this.m_variation = (VARIATION) variation;
	}

	@Override public String modelFamily() {
		return "keras_vgg";
	}

	@Override public String modelPrettyName() {
		switch (m_variation) {
			case VGG16:
				return "KerasVGG16";
			case VGG19:
				return "KerasVGG19";
			default:
				return null;
		}
	}
}
