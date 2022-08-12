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
 * VGG16.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.zoo;

import org.deeplearning4j.nn.conf.CacheMode;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.zoo.ZooModel;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import weka.core.OptionMetadata;
import weka.dl4j.Preferences;
import weka.dl4j.enums.PretrainedType;
import weka.dl4j.zoo.keras.VGG;

/**
 * A WEKA version of DeepLearning4j's VGG16 ZooModel.
 * Pretrained weights notes:
 * VGG16 CIFAR10 -  Download link possibly broken on DL4J end?
 * The downloaded zip for these weights is only 10mb vs 513mb for Imagenet
 *
 * @author Steven Lang
 * @author Rhys Compton
 */
public class Dl4jVGG extends AbstractZooModel {

	/**
	 * Unique ID for this version of the model.
	 */
	private static final long serialVersionUID = -4741420712433849216L;

	/**
	 * Desired version of the model.
	 */
	protected VGG.VARIATION m_variation = VGG.VARIATION.VGG16;

	/**
	 * Instantiate the model.
	 */
	public Dl4jVGG() {
		this.setPretrainedType(PretrainedType.IMAGENET);
	}

	@OptionMetadata(description = "The model variation to use.", displayName = "Model Variation", commandLineParamName = "variation", commandLineParamSynopsis = "-variation <String>") public VGG.VARIATION getVariation() {
		return m_variation;
	}

	@Override public ImagePreProcessingScaler getImagePreprocessingScaler() {
		return null;
	}

	public void setVariation(VGG.VARIATION var) {
		m_variation = var;

		setPretrainedType(m_pretrainedType);
	}

	@Override public void setPretrainedType(PretrainedType pretrainedType) {
		super.setPretrainedType(pretrainedType);
		setNumFExtractOutputs(4096);
		if (m_variation == VGG.VARIATION.VGG16 && pretrainedType == PretrainedType.VGGFACE) {
			// VGGFace pretrained has slightly different network structure to Imagenet pretrained
			setFeatureExtractionLayer("fc7");
			setOutputLayer("fc8");
		} else {
			setFeatureExtractionLayer("fc2");
			setOutputLayer("predictions");
		}
	}

	public ComputationGraph init(int numLabels, long seed, int[] shape, boolean filterMode) {
		ZooModel net = null;
		if (m_variation == VGG.VARIATION.VGG16) {
			net = org.deeplearning4j.zoo.model.VGG16.builder().cacheMode(CacheMode.NONE)
					.workspaceMode(Preferences.WORKSPACE_MODE).inputShape(shape).numClasses(numLabels).build();
		} else if (m_variation == VGG.VARIATION.VGG19) {
			net = org.deeplearning4j.zoo.model.VGG19.builder().cacheMode(CacheMode.NONE)
					.workspaceMode(Preferences.WORKSPACE_MODE).inputShape(shape).numClasses(numLabels).build();
		}

		ComputationGraph defaultNet = net.init();

		return initZooModel(net, defaultNet, seed, numLabels, filterMode);
	}

	@Override public int[] getInputShape() {
		if (m_variation == VGG.VARIATION.VGG16)
			return org.deeplearning4j.zoo.model.VGG16.builder().build().metaData().getInputShape()[0];
		else
			return org.deeplearning4j.zoo.model.VGG19.builder().build().metaData().getInputShape()[0];
	}
}