/*
 * WekaDeeplearning4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WekaDeeplearning4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WekaDeeplearning4j.  If not, see <https://www.gnu.org/licenses/>.
 *
 * EarlyStopping.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.earlystopping;

import java.io.Serializable;
import java.util.Enumeration;

import lombok.extern.log4j.Log4j2;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import weka.dl4j.Utils;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;

/**
 * Early stopping implementation to stop training after N epochs without loss improvement on a
 * separate validation set.
 *
 * @author Steven Lang
 */
@Log4j2 public class EarlyStopping implements OptionHandler, Serializable {

	/**
	 * SerialVersionUID
	 */
	private static final long serialVersionUID = 5248828973394650102L;

	/**
	 * Maximum of epochs without improvement
	 */
	private int maxEpochsNoImprovement = 0;

	/**
	 * Counter for the number of epochs without improvement
	 */
	private int countEpochsNoImprovement = 0;

	/**
	 * Last best score
	 */
	private double lastBestScore = Double.MAX_VALUE;

	/**
	 * Percentage of the training data to use as validation set
	 */
	private double validationSetPercentage = 0;

	/**
	 * Validation dataset
	 */
	private transient DataSetIterator valDataSetIterator;

	public EarlyStopping() {
	}

	/**
	 * Constructor setting maxEpochsNoImprovement and validation split
	 *
	 * @param maxEpochsNoImprovement  Maximum numer of epochs with no improvement
	 * @param validationSetPercentage Validation split percentage
	 */
	public EarlyStopping(int maxEpochsNoImprovement, double validationSetPercentage) {
		this.maxEpochsNoImprovement = maxEpochsNoImprovement;
		this.validationSetPercentage = validationSetPercentage;
	}

	/**
	 * Initialize the underlying dl4j EarlyStopping object
	 *
	 * @param dsIt DataSet trainIterator of the validation set
	 */
	public void init(DataSetIterator dsIt) {
		this.valDataSetIterator = dsIt;
	}

	/**
	 * Reset the counter
	 */
	private void resetEpochCounter() {
		countEpochsNoImprovement = 0;
	}

	/**
	 * Evaluate a model and check if the training should continue. Returns false if the score has not
	 * improved for the given number of epochs. Else true
	 *
	 * @param model Model to be evaluated against the validation set
	 * @return If training should continue or not
	 */
	public boolean evaluate(ComputationGraph model) {
		try {
			// Do not evaluate if set to zero
			if (maxEpochsNoImprovement == 0) {
				return true;
			}

			// If validation dataset is empty, do not evaluate and just continue
			if (!valDataSetIterator.hasNext()) {
				return true;
			}

			double score = Utils.computeScore(model, valDataSetIterator);
			if (score < lastBestScore) {
				resetEpochCounter();
				lastBestScore = score;
				return true;
			} else {
				countEpochsNoImprovement++;
				return countEpochsNoImprovement < maxEpochsNoImprovement;
			}

		} catch (Exception e) {
			log.error("Could not evaluate early stopping. Continuing training " + "process", e);
			return true;
		} finally {
			valDataSetIterator.reset();
		}
	}

	public int getMaxEpochsNoImprovement() {
		return maxEpochsNoImprovement;
	}

	@OptionMetadata(displayName = "max epochs with no improvement", description = "Terminate after N epochs in which the model has shown no improvement (default = 0).", commandLineParamName = "maxEpochsNoImprovement", commandLineParamSynopsis = "-maxEpochsNoImprovement <int>", displayOrder = 0) public void setMaxEpochsNoImprovement(
			int maxEpochsNoIMprovement) {
		if (maxEpochsNoIMprovement < 0) {
			throw new RuntimeException(
					"Early stopping criterion must be at " + "least zero or above. Negative values are not allowed.");
		}
		this.maxEpochsNoImprovement = maxEpochsNoIMprovement;
	}

	public double getValidationSetPercentage() {
		return validationSetPercentage;
	}

	@OptionMetadata(displayName = "validation set percentage", description = "Percentage of training set to use for validation (default = 0).", commandLineParamName = "valPercentage", commandLineParamSynopsis = "-valPercentage <float>", displayOrder = 1) public void setValidationSetPercentage(
			double p) {
		if (Double.compare(p, 100) >= 0 || p < 0) {
			throw new RuntimeException("Validation split percentage must be in 0 < p < 100.");
		}
		this.validationSetPercentage = p;
	}

	/**
	 * Get the validation dataset iterator
	 *
	 * @return DataSetIterator for the validation set
	 */
	public DataSetIterator getValDataSetIterator() {
		return valDataSetIterator;
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

	/**
	 * Returns a string describing this search method
	 *
	 * @return a description of the search method suitable for displaying in the explorer/experimenter
	 * gui
	 */
	public String globalInfo() {
		return "This options allows to stop the training process"
				+ "as soon as the loss does not improve anymore for N epochs. "
				+ "The loss is evaluated on a validation set. This set is created"
				+ "by removing the given percentage from the training data.";
	}
}
