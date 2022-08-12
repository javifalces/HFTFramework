package weka.dl4j.zoo.keras;

import org.deeplearning4j.zoo.ModelMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class for the different versions of EfficientNet. Has the same interface as Dl4j zoo models, so we can
 * simply call initPretrained().
 */
public class EfficientNet extends KerasZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = -3638980669539385867L;

	/**
	 * Different variations of the model.
	 */
	public enum VARIATION {
		/**
		 * B0 Size of EfficientNet.
		 */
		EFFICIENTNET_B0,
		/**
		 * B1 Size of EfficientNet.
		 */
		EFFICIENTNET_B1,
		/**
		 * B2 Size of EfficientNet.
		 */
		EFFICIENTNET_B2,
		/**
		 * B3 Size of EfficientNet.
		 */
		EFFICIENTNET_B3,
		/**
		 * B4 Size of EfficientNet.
		 */
		EFFICIENTNET_B4,
		/**
		 * B5 Size of EfficientNet.
		 */
		EFFICIENTNET_B5,
		/**
		 * B6 Size of EfficientNet.
		 */
		EFFICIENTNET_B6,
		/**
		 * B7 Size of EfficientNet.
		 */
		EFFICIENTNET_B7,
	}

	;

	/**
	 * Default input shape of the model.
	 */
	public static int[] inputShape = new int[] { 3, 224, 224 };

	/**
	 * Default variation of the model.
	 */
	protected VARIATION m_variation = VARIATION.EFFICIENTNET_B0;

	/**
	 * Instantiate the model.
	 */
	public EfficientNet() {
		setVariation(VARIATION.EFFICIENTNET_B0);
	}

	@Override public void setVariation(Enum variation) {
		this.m_variation = (VARIATION) variation;
	}

	@Override public String modelFamily() {
		return "keras_efficientnet";
	}

	@Override public String modelPrettyName() {
		switch (m_variation) {
			case EFFICIENTNET_B0:
				return "KerasEfficientNetB0";
			case EFFICIENTNET_B1:
				return "KerasEfficientNetB1";
			case EFFICIENTNET_B2:
				return "KerasEfficientNetB2";
			case EFFICIENTNET_B3:
				return "KerasEfficientNetB3";
			case EFFICIENTNET_B4:
				return "KerasEfficientNetB4";
			case EFFICIENTNET_B5:
				return "KerasEfficientNetB5";
			case EFFICIENTNET_B6:
				return "KerasEfficientNetB6";
			case EFFICIENTNET_B7:
				return "KerasEfficientNetB7";
			default:
				return null;
		}
	}
}
