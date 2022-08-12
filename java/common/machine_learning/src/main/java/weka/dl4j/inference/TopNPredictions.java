package weka.dl4j.inference;

import org.apache.commons.lang.StringUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import weka.core.WekaException;

import java.util.Arrays;

/**
 * Holds an arbitrary number of predictions, ordered by class probability.
 *
 * @author - Rhys Compton
 */
public class TopNPredictions {

	/**
	 * Number of predictions to hold.
	 */
	protected int n = 5;

	/**
	 * Predictions, ordered by class probability.
	 */
	protected Prediction[] topNPredictions;

	/**
	 * Name of the image used for prediction.
	 */
	protected String imageName = "";

	/**
	 * Name of the model used for prediction.
	 */
	protected String modelName = "";

	/**
	 * Initialize.
	 */
	public TopNPredictions() {
	}

	/**
	 * Initialize the top N predictions.
	 *
	 * @param imageName Name of image
	 * @param modelName Name of model
	 */
	public TopNPredictions(String imageName, String modelName) {
		this.imageName = imageName;
		this.modelName = modelName;
	}

	/**
	 * Initialize the prediction array.
	 */
	protected void initPredArray() {
		this.topNPredictions = new Prediction[n];
	}

	/**
	 * Main entrypoint, decodes predictions into a TopNPredictions object.
	 *
	 * @param predictions Raw model predictions
	 * @param classes     Class map
	 * @throws Exception image exception
	 */
	public void process(INDArray predictions, String[] classes) throws Exception {
		initPredArray();

		double[] predDouble = predictions.toDoubleVector();

		if (predDouble.length != classes.length) {
			throw new WekaException(String.format("Number of prediction classes from model (%d) "
							+ "don't match size of class map (%d)! Have you set the correct class map?", predDouble.length,
					classes.length));
		}

		// Get the Top N prediction indices
		int[] bestNIndices = getBestNIndices(predDouble);

		// Create Prediction objects for each index (starting from highest probability)
		for (int i = 0; i < bestNIndices.length; i++) {
			int topNIndex = bestNIndices[i];
			String className = classes[topNIndex];
			double classProb = predictions.getDouble(topNIndex);

			Prediction p = new Prediction(topNIndex, className, classProb);

			this.topNPredictions[i] = p;
		}
	}

	/**
	 * Calculate the longest predicted class name - used for formatting the output table
	 *
	 * @return Longest class name length
	 */
	private int getMaxLenClassName() {
		// Default length is 15
		int maxLen = 15;
		for (Prediction p : topNPredictions) {
			int tmpLen = p.getClassName().length();
			if (tmpLen > maxLen) {
				maxLen = tmpLen;
			}
		}
		return maxLen;
	}

	/**
	 * Gets a string representing a <br> with the given breakChar.
	 *
	 * @param breakChar Character to repeat
	 * @param len       Length of the line break
	 * @return String of the given length
	 */
	private String getTableBreak(String breakChar, int len) {
		return StringUtils.repeat(breakChar, len) + "\n";
	}

	/**
	 * Return a summary string of the stored predictions.
	 *
	 * @return Results in table format
	 */
	public String toSummaryString() {
		return toSummaryString(imageName, modelName);
	}

	/**
	 * Return a summary string of the stored predictions, headed with the given image and model name.
	 *
	 * @param imageName    Image used for prediction
	 * @param zooModelName Model used for prediction
	 * @return Results in table format
	 */
	public String toSummaryString(String imageName, String zooModelName) {
		// Init some objects we'll need
		StringBuilder text = new StringBuilder();

		int maxLenClassName = getMaxLenClassName();

		String titleClassID = "Class ID";
		String titleClassName = "Class Name";
		String titleProbability = "Prob %";

		// The class name column should be based on the maximum length of the top 5 class names
		String formattedClassName = "%" + maxLenClassName + "s"; // --> " %20s "

		// Format used for each line in the table, keeps all columns equally sized
		String lineFormat = "%10s | " + formattedClassName + " | %10s\n"; // --> "%10s | %20s | %10s"

		// -> "Class ID |               Class Name |     Prob %"
		String columnHeaders = String.format(lineFormat, titleClassID, titleClassName, titleProbability);

		// TOTAL length of the lines
		int lineLength = columnHeaders.length();

		// Add the top table break
		text.append(getTableBreak("=", lineLength));

		// Add the image name and the model used for prediction
		text.append(imageName).append(" - ").append(zooModelName).append("\n\n");

		// Generate the column headers
		text.append(columnHeaders);
		// Append the header/content break
		text.append(getTableBreak("-", lineLength));

		// Add each row to the output table
		for (Prediction p : topNPredictions) {
			text.append(p.toTableRowString(lineFormat));
		}

		// Finish off with a bottom table break
		text.append(getTableBreak("=", lineLength));

		return text.toString();
	}

	/**
	 * Get the indices of the top n highest values from the input array.
	 *
	 * @param array Array to find highest values from
	 * @return indices of highest values
	 */
	private int[] getBestNIndices(double[] array) {
		//create sort able array with index and value pair
		IndexValuePair[] pairs = new IndexValuePair[array.length];
		for (int i = 0; i < array.length; i++) {
			pairs[i] = new IndexValuePair(i, array[i]);
		}

		//sort
		Arrays.sort(pairs, (o1, o2) -> Double.compare(o2.value, o1.value));

		//extract the indices
		int[] result = new int[n];
		for (int i = 0; i < n; i++) {
			result[i] = pairs[i].index;
		}
		return result;
	}

	/**
	 * Helper class for finding highest indices.
	 *
	 * @author - Rhys Compton
	 */
	private static class IndexValuePair {

		/**
		 * Index of the value.
		 */
		private final int index;
		/**
		 * Value.
		 */
		private final double value;

		/**
		 * Initialize the index value pair.
		 *
		 * @param index Index
		 * @param value Value
		 */
		public IndexValuePair(int index, double value) {
			this.index = index;
			this.value = value;
		}
	}

	public int getN() {
		return this.n;
	}

	public void setN(int n) {
		this.n = n;
	}

	/**
	 * Get the prediction at index i.
	 *
	 * @param i Index of prediction to get.
	 * @return Prediction.
	 */
	public Prediction getPrediction(int i) {
		return topNPredictions[i];
	}

	public Prediction getTopPrediction() {
		return topNPredictions[0];
	}

	public Prediction[] getTopPredictions() {
		return topNPredictions;
	}
}
