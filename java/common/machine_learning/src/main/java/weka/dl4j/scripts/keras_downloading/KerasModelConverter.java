package weka.dl4j.scripts.keras_downloading;

import lombok.extern.log4j.Log4j2;
import org.deeplearning4j.nn.conf.CNN2DFormat;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasLayer;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import weka.dl4j.layers.lambda.CustomBroadcast;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class loads in a folder of Keras files, and one by one converts them
 * into the native DL4J format (.zip). This is safer to work with in DL4J than
 * importing from Keras files every time, and is fine to do in this case because
 * WDL4J defines a fixed set of models - this process only needs to be done once.
 */
@Log4j2 public class KerasModelConverter {

	/**
	 * File path to model summaries.
	 */
	private static String modelSummariesPath = "";

	/**
	 * Regex for the broadcast layer.
	 */
	private static final String broadcastLayerRegex = "^broadcast_w(\\d+).*";

	/**
	 * Save the supplied model .h5 file into our output folder.
	 *
	 * @param modelFile    .h5 file
	 * @param outputFolder Folder to save file into.
	 */
	private static void saveH5File(File modelFile, File outputFolder) {
		try {
			INDArray testShape = Nd4j.zeros(1, 3, 224, 224);
			String modelName = modelFile.getName();
			Method method = null;
			try {
				method = InputType.class.getMethod("setDefaultCNN2DFormat", CNN2DFormat.class);
				method.invoke(null, CNN2DFormat.NCHW);
			} catch (NoSuchMethodException ex) {
				log.error("setDefaultCNN2DFormat() not found on InputType class... "
						+ "Are you using the custom built deeplearning4j-nn.jar?");
				System.exit(1);
			}

			if (modelName.contains("EfficientNet")) {
				// Fixes for EfficientNet family of models
				testShape = Nd4j.zeros(1, 224, 224, 3);
				method.invoke(null, CNN2DFormat.NHWC);
				// We don't want the resulting .zip files to have 'Fixed' in the name, so we'll strip it off here
				modelName = modelName.replace("Fixed", "");
			}
			ComputationGraph kerasModel = KerasModelImport.importKerasModelAndWeights(modelFile.getAbsolutePath());
			kerasModel.feedForward(testShape, false);
			// e.g. ResNet50.h5 -> KerasResNet50.zip
			modelName = "Keras" + modelName.replace(".h5", ".zip");
			String newZip = Paths.get(outputFolder.getPath(), modelName).toString();
			kerasModel.save(new File(newZip));
			System.out.println("Saved file " + newZip);
		} catch (Exception e) {
			log.error("\n\nCouldn't save " + modelFile.getName());
			e.printStackTrace();
		}
	}

	/**
	 * Main entrypoint.
	 *
	 * @param args args
	 * @throws Exception exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: KerasModelConverter <h5 folder path> <model summary folder path>");
			System.exit(1);
		}

		// Default location where Keras models are saved
		String modelFolderPath = args[0];
		modelSummariesPath = args[1];

		File modelFolder = new File(modelFolderPath);
		File outputFolder = new File(Paths.get(modelFolder.getParent(), "dl4j_format").toString());
		if (outputFolder.mkdir())
			System.out.println("Created DL4J format folder at " + outputFolder.getPath());

		File[] modelFiles = modelFolder.listFiles();
		if (modelFiles == null) {
			throw new Exception("Invalid folder name: " + modelFolderPath);
		}
		Arrays.sort(modelFiles);

		loadLambdaLayers();

		for (File fileEntry : modelFiles) {
			if (fileEntry.getPath().endsWith(".h5")) {
				saveH5File(fileEntry, outputFolder);
			}
		}
	}

	/**
	 * Checks whether this line in the model summary is a broadcast layer.
	 *
	 * @param line Line to check.
	 * @return True if layer is broadcast layer.
	 */
	private static boolean isBroadcastLayer(String line) {
		Pattern p = Pattern.compile(broadcastLayerRegex);
		Matcher m = p.matcher(line);

		return m.matches();
	}

	/**
	 * Gets the width/height of the supplied broadcast layer.
	 *
	 * @param layerName Layer name to check
	 * @return Width of layer.
	 * @throws Exception If we couldn't find the width in the supplied layername.
	 */
	private static int getWidth(String layerName) throws Exception {
		Pattern p = Pattern.compile(broadcastLayerRegex);
		Matcher m = p.matcher(layerName);

		if (m.find()) {
			String width = m.group(1);
			return Integer.parseInt(width);
		}
		throw new Exception("Couldn't find width in layerName " + layerName);
	}

	/**
	 * Register lambda layers for the given model summaries.
	 *
	 * @throws Exception File exception.
	 */
	private static void loadLambdaLayers() throws Exception {
		File[] modelSummaries = new File(modelSummariesPath).listFiles();
		assert modelSummaries != null;
		Arrays.sort(modelSummaries);

		for (File f : modelSummaries) {
			BufferedReader br = new BufferedReader(new FileReader(f.getAbsoluteFile()));
			String modelName = f.getName();
			String line;
			while ((line = br.readLine()) != null) {
				//          __________________________________________________________________________________________________
				// Line is~ block2c_se_expand (Conv2D)      (None, 1, 1, 144)    1008        block2c_se_reduce[0][0]
				//          __________________________________________________________________________________________________
				if (isBroadcastLayer(line)) {
					String[] lineParts = line.split(" ");
					String layerName = lineParts[0]; // -> broadcast_w65_d144_2
					int width = getWidth(layerName);
					KerasLayer.registerLambdaLayer(layerName, new CustomBroadcast(width));
					System.out.println(
							String.format("Registered %s layer %s with width %d", modelName, layerName, width));
				}
			}
		}
	}
}
