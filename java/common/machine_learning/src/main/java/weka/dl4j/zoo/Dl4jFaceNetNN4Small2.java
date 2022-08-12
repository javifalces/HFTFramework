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
 * FaceNetNN4Small2.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.zoo;

import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.dl4j.Preferences;
import weka.dl4j.enums.PretrainedType;

/**
 * A WEKA version of DeepLearning4j's FaceNetNN4Small2 ZooModel.
 *
 * @author Steven Lang
 * @author Rhys Compton
 */
public class Dl4jFaceNetNN4Small2 extends AbstractZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = -3489424545896301678L;

	/**
	 * Instantiate the model.
	 */
	public Dl4jFaceNetNN4Small2() {
		setPretrainedType(PretrainedType.NONE);
	}

	@Override public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
		org.deeplearning4j.zoo.model.FaceNetNN4Small2 net = org.deeplearning4j.zoo.model.FaceNetNN4Small2.builder()
				.cacheMode(CacheMode.NONE).workspaceMode(Preferences.WORKSPACE_MODE).inputShape(shape)
				.numClasses(numLabels).build();

		ComputationGraph defaultNet = net.init();

		return initZooModel(net, defaultNet, seed, numLabels, filterMode);
	}

	@Override public int[] getInputShape() {
		return org.deeplearning4j.zoo.model.FaceNetNN4Small2.builder().build().metaData().getInputShape()[0];
	}

	/**
	 * Get the current variation of the zoo model (e.g., Resnet50 or Resnet101)
	 *
	 * @return Variation
	 */
	@Override public Enum getVariation() {
		return null;
	}

	@Override public ImagePreProcessingScaler getImagePreprocessingScaler() {
		return null;
	}
}
