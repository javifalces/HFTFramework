package weka.dl4j.zoo.keras;

import org.deeplearning4j.zoo.ModelMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class for the different versions of DenseNet. Has the same interface as Dl4j zoo models, so we can
 * simply call initPretrained().
 */
public class DenseNet extends KerasZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = 982160047744739299L;

	/**
	 * Different variations of the model.
	 */
	public enum VARIATION {
		/**
		 * DenseNet121 model variation.
		 */
		DENSENET121,
		/**
		 * DENSENET169 model variation.
		 */
		DENSENET169,
		/**
		 * DENSENET201 model variation.
		 */
		DENSENET201
	}

	;

	/**
	 * Default input shape of the model.
	 */
	public static int[] inputShape = new int[] { 3, 224, 224 };

	/**
	 * Default variation of the model.
	 */
	protected VARIATION m_variation = VARIATION.DENSENET121;

	/**
	 * Instantiate the model.
	 */
	public DenseNet() {
		setVariation(VARIATION.DENSENET121);
	}

	@Override public void setVariation(Enum variation) {
		this.m_variation = (VARIATION) variation;
	}

	@Override public String modelFamily() {
		return "keras_densenet";
	}

	@Override public String modelPrettyName() {
		switch (m_variation) {
			case DENSENET121:
				return "KerasDenseNet121";
			case DENSENET169:
				return "KerasDenseNet169";
			case DENSENET201:
				return "KerasDenseNet201";
			default:
				return null;
		}
	}
}
