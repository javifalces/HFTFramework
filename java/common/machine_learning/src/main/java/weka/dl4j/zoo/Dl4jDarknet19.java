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
 * Darknet19.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.zoo;

import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.core.OptionMetadata;
import weka.dl4j.Preferences;
import weka.dl4j.enums.PretrainedType;

/**
 * A WEKA version of DeepLearning4j's Darknet19 ZooModel.
 *
 * @author Steven Lang
 * @author Rhys Compton
 */
public class Dl4jDarknet19 extends AbstractZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = 7854379460490744564L;

	/**
	 * Variations of the model.
	 */
	public enum VARIATION {
		/**
		 * Model with 3x224x224 input size.
		 */
		INPUT224,
		/**
		 * Model with 3x448x448 input size.
		 */
		INPUT448
	}

	;

	/**
	 * Desired version of the model.
	 */
	protected VARIATION m_variation = VARIATION.INPUT224;

	/**
	 * Instantiate the model.
	 */
	public Dl4jDarknet19() {
		setPretrainedType(PretrainedType.IMAGENET);
		setNumFExtractOutputs(1000);
		setFeatureExtractionLayer("globalpooling");
		setOutputLayer("softmax");
		setExtraLayersToRemove(new String[] { "loss" });
	}

	@OptionMetadata(description = "The model variation to use.", displayName = "Model Variation", commandLineParamName = "variation", commandLineParamSynopsis = "-variation <String>") public VARIATION getVariation() {
		return m_variation;
	}

	@Override public ImagePreProcessingScaler getImagePreprocessingScaler() {
		return new ImagePreProcessingScaler(0, 1);
	}

	public void setVariation(VARIATION var) {
		m_variation = var;
	}

	@Override public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
		org.deeplearning4j.zoo.model.Darknet19 net = org.deeplearning4j.zoo.model.Darknet19.builder()
				.cacheMode(CacheMode.NONE).workspaceMode(Preferences.WORKSPACE_MODE).inputShape(shape)
				.numClasses(numLabels).build();

		ComputationGraph defaultNet = net.init();

		return initZooModel(net, defaultNet, seed, numLabels, filterMode);
	}

	@Override public int[] getInputShape() {
		int imgSize = -1;
		if (m_variation == VARIATION.INPUT224) {
			imgSize = 224;
		} else if (m_variation == VARIATION.INPUT448) {
			imgSize = 448;
		}
		int[] shape = new int[] { 3, imgSize, imgSize };
		return shape;
	}
}
