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
 * EpochListener.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.listener;

import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.extern.log4j.Log4j2;
import org.deeplearning4j.datasets.iterator.AsyncDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.eval.RegressionEvaluation;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.CachingDataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import weka.dl4j.Utils;
import weka.core.OptionMetadata;

/**
 * A listener that prints the model score every epoch. Inspired by ScoreIterationListener written by
 * Adam Gibson.
 *
 * @author Steven Lang
 */
@Log4j2 public class EpochListener extends TrainingListener {

	private static final long serialVersionUID = -8852994767947925554L;

	/** Epoch counter */
	// private int currentEpoch = 0;

	/**
	 * Evaluate every N epochs
	 */
	private int n = 5;

	/**
	 * Enable intermediate evaluations
	 */
	private boolean isIntermediateEvaluationsEnabled = true;

	@Override public void onEpochEnd(Model model) {
		currentEpoch++;

		// Skip if this is not an evaluation epoch
		if (currentEpoch % n != 0) {
			return;
		}

		String s = "Epoch [" + currentEpoch + "/" + numEpochs + "]\n";

		if (isIntermediateEvaluationsEnabled) {
			s += "Train Set:      \n" + evaluateDataSetIterator(model, trainIterator, true);
			if (validationIterator != null) {
				s += "Validation Set: \n" + evaluateDataSetIterator(model, validationIterator, false);
			}
		}

		log(s);
	}

	/**
	 * Evaluate an iterator on a given model
	 *
	 * @param model    Model which is to be evaluated
	 * @param iterator Iterator yielding datasets
	 * @param train    Whether this is a training or validation iterator
	 * @return Evaluation string for logging
	 */
	private String evaluateDataSetIterator(Model model, DataSetIterator iterator, boolean train) {
		iterator.reset();
		String s = "";
		try {
			boolean isClassification = numClasses > 1;
			if (model instanceof ComputationGraph) {
				ComputationGraph net = (ComputationGraph) model;

				Evaluation classificationEvaluation = new Evaluation(numClasses);
				RegressionEvaluation regressionEvaluation = new RegressionEvaluation(numClasses);
				while (iterator.hasNext()) {
					DataSet next;
					// AsyncDataSetIterator and CachingDataSetIterator do not support next(num)
					if (iterator instanceof AsyncDataSetIterator || iterator instanceof CachingDataSetIterator) {
						next = Utils.getNext(iterator);
					} else {
						final int batch = iterator.batch() * 8;
						next = Utils.getNext(iterator, batch);
					}
					INDArray output = net.outputSingle(next.getFeatures()); // get the networks prediction
					if (isClassification) {
						classificationEvaluation.eval(next.getLabels(), output, next.getLabelsMaskArray());
					} else {
						regressionEvaluation.eval(next.getLabels(), output, next.getLabelsMaskArray());
					}
				}

				// Add loss (denoted as score in dl4j)
				final double score;
				if (train) {
					score = net.score();
				} else {
					score = Utils.computeScore(net, iterator);
				}

				s += String.format(" Loss:           %9f" + System.lineSeparator(), score);

				// Add Dl4j metrics
				if (isClassification) {
					final String stats = Arrays.stream(classificationEvaluation.stats().split(System.lineSeparator()))
							.filter(line -> !line.contains("# of classes")) // Remove # classes line
							.filter(line -> !line.contains("===")) // Remove separators
							.filter(line -> !line.contains("Predictions labeled as")) // Remove confusion matrix
							.filter(line -> !line.trim().isEmpty()) // Remove empty lines
							.collect(Collectors.joining(System.lineSeparator())); // Join to original format
					s += stats + System.lineSeparator();
				} else {
					s += regressionEvaluation.stats() + System.lineSeparator();
				}
			}
		} catch (UnsupportedOperationException e) {
			return "Set is too small and does not contain all labels." + System.lineSeparator();
		} catch (Exception e) {
			log.error("Evaluation after epoch failed. Error: ", e);
			return "Not available";
		} finally {
			iterator.reset();
		}

		return s;
	}

	@Override public void log(String msg) {
		log.info(msg);
	}

	public int getN() {
		return this.n;
	}

	@OptionMetadata(displayName = "evaluate every N epochs", description = "Evaluate every N epochs (default = 5).", commandLineParamName = "n", commandLineParamSynopsis = "-n <int>", displayOrder = 0) public void setN(
			int evaluateEveryNEpochs) {
		if (evaluateEveryNEpochs < 1) {
			// Never evaluate
			this.n = Integer.MAX_VALUE;
		} else {
			this.n = evaluateEveryNEpochs;
		}
	}

	@OptionMetadata(displayName = "enable intermediate evaluations", description = "Enable intermediate evaluations (default = true).", commandLineParamName = "eval", commandLineParamSynopsis = "-eval <boolean>", displayOrder = 0) public boolean isIntermediateEvaluationsEnabled() {
		return isIntermediateEvaluationsEnabled;
	}

	public void setIntermediateEvaluationsEnabled(boolean intermediateEvaluationsEnabled) {
		this.isIntermediateEvaluationsEnabled = intermediateEvaluationsEnabled;
	}

	/**
	 * Returns a string describing this search method
	 *
	 * @return a description of the search method suitable for displaying in the explorer/experimenter
	 * gui
	 */
	public String globalInfo() {
		return "A listener which evaluates the model while training every N " + "epochs.";
	}
}
