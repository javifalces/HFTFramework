package weka.dl4j.inference;

/**
 * Built-in class maps for WDL4J.
 */
public enum ClassmapType {
	/**
	 * Standard ImageNet classmap.
	 */
	IMAGENET,
	/**
	 * ImageNet classmap which DarkNet19 was trained on.
	 */
	DARKNET_IMAGENET,

	/**
	 * Classmap for the VGGFace dataset.
	 */
	VGGFACE,
	/**
	 * Generic flag for any custom dataset supplied by the user.
	 */
	CUSTOM
}
