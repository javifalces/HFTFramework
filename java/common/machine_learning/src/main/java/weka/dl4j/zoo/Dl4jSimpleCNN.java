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
 * SimpleCNN.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.zoo;

import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.zoo.PretrainedType;
import weka.dl4j.Preferences;

/**
 * A WEKA version of DeepLearning4j's SimpleCNN ZooModel.
 *
 * @author Steven Lang
 */
public class Dl4jSimpleCNN {
	// Removed as default network doesn't have output layer
	// Also unneeded in the model zoo, AlexNet is a simpler model architecture
	// So if doing debugging and one wants a simple NN, use AlexNet instead

	//public class Dl4jSimpleCNN extends AbstractZooModel {
	//  private static final long serialVersionUID = -7543565465157698715L;
	//
	//  @Override
	//  public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
	//    org.deeplearning4j.zoo.model.SimpleCNN net = org.deeplearning4j.zoo.model.SimpleCNN.builder()
	//        .cacheMode(CacheMode.NONE)
	//        .workspaceMode(Preferences.WORKSPACE_MODE)
	//        .inputShape(shape)
	//        .numClasses(numLabels)
	//        .build();
	//
	//    ComputationGraph defaultNet = ((MultiLayerNetwork) net.init()).toComputationGraph();
	//
	//    return attemptToLoadWeights(net, defaultNet, seed, numLabels, filterMode);
	//  }
	//
	//  @Override
	//  public int[][] getShape() {
	//    return org.deeplearning4j.zoo.model.SimpleCNN.builder().build().metaData().getInputShape();
	//  }
}
