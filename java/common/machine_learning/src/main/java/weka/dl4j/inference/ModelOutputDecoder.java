package weka.dl4j.inference;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.indexing.NDArrayIndex;
import weka.core.converters.CSVLoader;
import weka.dl4j.ResourceResolver;
import weka.dl4j.Utils;
import weka.core.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * Decodes model outputs into a human-readable and more workable format.
 * Holds the class map to be used for decoding
 *
 * @author - Rhys Compton
 */
@Log4j2 public class ModelOutputDecoder implements Serializable, OptionHandler {

	/**
	 * Class Map to use to decode the model output.
	 */
	protected ClassmapType builtInClassMap = ClassmapType.IMAGENET;

	/**
	 * Path to custom class map file.
	 */
	protected File classMapFile = new File(Utils.defaultFileLocation());

	/**
	 * Decode predictions, without any image or model name.
	 *
	 * @param predictions Predictions to decode
	 * @return Parsed predictions.
	 * @throws Exception Exception.
	 */
	public TopNPredictions decodePredictions(INDArray predictions) throws Exception {
		return decodePredictions(predictions, "", "");
	}

	/**
	 * Main entrypoint - decode the model predictions, saving the image and model name alongside it.
	 *
	 * @param predictions Predictions to decode
	 * @param imageName   Name of the image used for prediction
	 * @param modelName   Name of the model used for prediction
	 * @return TopNPredictions object, parsed from the predictions
	 * @throws Exception exception
	 */
	public TopNPredictions decodePredictions(INDArray predictions, String imageName, String modelName)
			throws Exception {
		// Get number of instances to predict for
		long[] shape = predictions.shape();

		if (shape.length == 1) {
			// Want arr to be [batch_size, numClasses], so reshape it to be like that
			predictions = reshapeSingleInstanceToBatch(predictions);
		}

		// Should only be 1 at the moment - only single image prediction is supported
		int numInstances = (int) shape[0];

		// Create the returning Prediction[]
		TopNPredictions[] result = new TopNPredictions[numInstances];

		// Decode each prediction
		for (int i = 0; i < numInstances; i++) {
			INDArray thisInstance = predictions.get(NDArrayIndex.point(i));

			TopNPredictions topNPredictions = new TopNPredictions(imageName, modelName);
			topNPredictions.process(thisInstance, getClasses());

			result[i] = topNPredictions;
		}

		// Only supporting single images atm
		return result[0];
	}

	/**
	 * Reshape the single instance INDArray to a batch of size 1.
	 *
	 * @param array Array to reshape
	 * @return Reshaped activation
	 */
	private INDArray reshapeSingleInstanceToBatch(INDArray array) {
		long numClasses = array.shape()[0];
		return array.reshape(1, numClasses);
	}

	/**
	 * Attempts to look in a couple of locations for the class-maps resource folder
	 *
	 * @return Correct class map folder
	 * @throws Exception If the class map folder cannot be found
	 */
	private String getClassMapFolder() throws Exception {
		// Try the current directory
		return new ResourceResolver().GetResolvedPath("class-maps");
	}

	/**
	 * Gets the path of the class map file - either the custom path or one of the built-in files
	 *
	 * @return Correct class map path
	 * @throws Exception If the class map file cannot be found
	 */
	private String getClassMapPath() throws Exception {
		// Return the custom file path if the user has specified it

		String classMapFolder = getClassMapFolder();
		String classMapPath = null;

		switch (this.builtInClassMap) {
			case CUSTOM:
				if (this.classMapFile != null) {
					classMapPath = this.classMapFile.getPath();
					break;
				}
			case IMAGENET:
				classMapPath = Paths.get(classMapFolder, "IMAGENET.txt").toString();
				break;
			case DARKNET_IMAGENET:
				classMapPath = Paths.get(classMapFolder, "DARKNET_IMAGENET.txt").toString();
				break;
			case VGGFACE:
				classMapPath = Paths.get(classMapFolder, "VGGFACE.txt").toString();
				break;
		}

		if (classMapPath == null) {
			throw new Exception("No class map file found, either specify a file "
					+ "using setClassMapFile(File) or use a built-in class map with "
					+ "setBuiltInClassMap(BuiltInClassMap)");
		}

		return classMapPath;
	}

	/**
	 * Parse a classmap from a valid .arff file.
	 *
	 * @param filepath Filepath to arff file
	 * @return Parsed classmap.
	 * @throws IOException file exception.
	 */
	public static String[] parseClassmapFromArff(String filepath) throws IOException {
		Instances instances = new Instances(new FileReader(filepath));
		return parseClassmap(instances);
	}

	/**
	 * Parse a classmap from a valid .csv dataset.
	 *
	 * @param filepath Filepath to a valid .csv dataset
	 * @return Parsed classmap.
	 * @throws IOException file exception.
	 */
	public static String[] parseClassmapFromCsv(String filepath) throws IOException {
		CSVLoader loader = new CSVLoader();
		loader.setSource(new File(filepath));
		loader.setNominalAttributes("last");
		Instances instances = loader.getDataSet();
		return parseClassmap(instances);
	}

	/**
	 * Get a classmap from a set of instances.
	 *
	 * @param instances Instances to parse classes from
	 * @return Parsed classmap.
	 */
	public static String[] parseClassmap(Instances instances) {
		instances.setClassIndex(instances.numAttributes() - 1);
		ArrayList<Object> list = Collections.list(instances.classAttribute().enumerateValues());
		return list.toArray(new String[0]);
	}

	/**
	 * Parses the classmap file into a String[]
	 *
	 * @return String[], one item for each class
	 * @throws Exception
	 */
	public String[] getClasses() {
		List<String> classes = new ArrayList<String>();
		try {
			String classMapPath = getClassMapPath();
			if (FilenameUtils.isExtension(classMapPath, "arff")) {
				return parseClassmapFromArff(classMapPath);
			}
			if (FilenameUtils.isExtension(classMapPath, "csv")) {
				return parseClassmapFromCsv(classMapPath);
			}
			try (FileReader fr = new FileReader(getClassMapPath())) {
				try (BufferedReader br = new BufferedReader(fr)) {
					// Create a class for each line in the file
					for (String line = br.readLine(); line != null; line = br.readLine()) {
						line = line.trim();
						// Ignore empty lines
						if (line.length() == 0)
							continue;

						classes.add(line);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return classes.toArray(new String[0]);
	}

	@OptionMetadata(displayName = "Built in class map", description =
			"A predefined class map based on a specific dataset (IMAGENET, VGGFACE). "
					+ "Useful when using a pretrained zoo model as these are often trained on IMAGENET.", commandLineParamName = "builtIn", commandLineParamSynopsis = "-builtIn {IMAGENET, VGGFACE, CUSTOM}") public ClassmapType getBuiltInClassMap() {
		return builtInClassMap;
	}

	public void setBuiltInClassMap(ClassmapType classMap) {
		this.builtInClassMap = classMap;
	}

	@OptionMetadata(displayName = "Class map file", description = "File containing mappings from class IDs to human-readable names - can be .txt or .arff", commandLineParamName = "classMapFile", commandLineParamSynopsis = "-classMapFile <file location>") public File getClassMapFile() {
		return classMapFile;
	}

	public void setClassMapFile(File f) {
		this.classMapFile = f;
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override public Enumeration<Option> listOptions() {
		return Option.listOptionsForClass(this.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override public String[] getOptions() {
		return Option.getOptions(this, this.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		Option.setOptions(options, this, this.getClass());
	}
}
