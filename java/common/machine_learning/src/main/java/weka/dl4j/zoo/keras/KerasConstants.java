package weka.dl4j.zoo.keras;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple class to hold URLs and checksums of all Keras models in WDL4J.
 */
public class KerasConstants {

	/**
	 * URLs of model files.
	 */
	public static Map<String, String> Locations;

	/**
	 * Checksums of model files.
	 */
	public static Map<String, Long> Checksums;

	static {
		Map<String, String> locations = new HashMap<>();
		Map<String, Long> checksums = new HashMap<>();

		// ########
		// DenseNet
		// ########
		locations.put("KerasDenseNet121",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasDenseNet121.zip");
		locations.put("KerasDenseNet169",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasDenseNet169.zip");
		locations.put("KerasDenseNet201",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasDenseNet201.zip");
		checksums.put("KerasDenseNet121", 3317961040L);
		checksums.put("KerasDenseNet169", 2917284844L);
		checksums.put("KerasDenseNet201", 390469819L);

		// ############
		// EfficientNet
		// ############
		locations.put("KerasEfficientNetB0",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasEfficientNetB0.zip");
		locations.put("KerasEfficientNetB1",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasEfficientNetB1.zip");
		locations.put("KerasEfficientNetB2",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasEfficientNetB2.zip");
		locations.put("KerasEfficientNetB3",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasEfficientNetB3.zip");
		locations.put("KerasEfficientNetB4",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasEfficientNetB4.zip");
		locations.put("KerasEfficientNetB5",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasEfficientNetB5.zip");
		locations.put("KerasEfficientNetB6",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasEfficientNetB6.zip");
		locations.put("KerasEfficientNetB7",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasEfficientNetB7.zip");
		checksums.put("KerasEfficientNetB0", 3915144300L);
		checksums.put("KerasEfficientNetB1", 2399258563L);
		checksums.put("KerasEfficientNetB2", 3826625376L);
		checksums.put("KerasEfficientNetB3", 4091966028L);
		checksums.put("KerasEfficientNetB4", 4208479803L);
		checksums.put("KerasEfficientNetB5", 2859282668L);
		checksums.put("KerasEfficientNetB6", 1628985300L);
		checksums.put("KerasEfficientNetB7", 1160697645L);

		// ###########
		// InceptionV3
		// ###########
		locations.put("KerasInceptionV3",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasInceptionV3.zip");
		checksums.put("KerasInceptionV3", 2527243775L);

		// ######
		// NASNet
		// ######
		locations.put("KerasNASNetMobile",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasNASNetMobile.zip");
		locations.put("KerasNASNetLarge",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasNASNetLarge.zip");
		checksums.put("KerasNASNetMobile", 1844533549L);
		checksums.put("KerasNASNetLarge", 3178325491L);

		// ######
		// ResNet
		// ######
		locations.put("KerasResNet50",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasResNet50.zip");
		locations.put("KerasResNet50V2",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasResNet50V2.zip");
		locations.put("KerasResNet101",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasResNet101.zip");
		locations.put("KerasResNet101V2",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasResNet101V2.zip");
		locations.put("KerasResNet152",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasResNet152.zip");
		locations.put("KerasResNet152V2",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasResNet152V2.zip");
		checksums.put("KerasResNet50", 3286468754L);
		checksums.put("KerasResNet50V2", 3513024472L);
		checksums.put("KerasResNet101", 220275721L);
		checksums.put("KerasResNet101V2", 3095544222L);
		checksums.put("KerasResNet152", 3085881774L);
		checksums.put("KerasResNet152V2", 2370199264L);

		// ###
		// VGG
		// ###
		locations.put("KerasVGG16",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasVGG16.zip");
		locations.put("KerasVGG19",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasVGG19.zip");
		checksums.put("KerasVGG16", 205199086L);
		checksums.put("KerasVGG19", 1716706036L);

		// ########
		// Xception
		// ########
		locations.put("KerasXception",
				"https://github.com/Waikato/wekaDeeplearning4j/releases/download/zoo-models/KerasXception.zip");
		checksums.put("KerasXception", 3786331101L);

		Locations = Collections.unmodifiableMap(locations);
		Checksums = Collections.unmodifiableMap(checksums);
	}
}
