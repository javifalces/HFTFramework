package weka.dl4j.zoo.keras;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.deeplearning4j.common.resources.DL4JResources;
import org.deeplearning4j.common.resources.ResourceType;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasLayer;
import org.deeplearning4j.nn.modelimport.keras.KerasModel;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.zoo.ModelMetaData;
import org.deeplearning4j.zoo.PretrainedType;
import org.deeplearning4j.zoo.ZooModel;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
 * This class essentially copies DL4J's ZooModel class, allowing the custom Keras models to provide the same interface
 * as those models provided by DL4J.
 * <p>
 * It was originally created as KerasZooModels were loaded from the .h5 file, so they couldn't use DL4J's ZooModel
 * implementation (as it loads model from the native .zip format). Loading from .h5 format is a little unreliable at times
 * so this class uses the native DL4J .zip format.
 * <p>
 * This implementation is different in that the pretrained URL and checksum are taken from the KerasConstants file
 * - a global constant which defines these values. This is easier to manage than having URLs and checksums in each
 * individual model class (as is done in DL4J).
 *
 * @author Rhys Compton
 */
@Log4j2 public abstract class KerasZooModel extends ZooModel implements Serializable {

	/**
	 * Unique version of class.
	 */
	private static final long serialVersionUID = -7117367016579062724L;

	/**
	 * Default input shape.
	 */
	protected int[] inputShape;

	/**
	 * Get the model family.
	 *
	 * @return String denoting the model family
	 */
	public abstract String modelFamily();

	/**
	 * Get the pretty name of the model (e.g., ResNet 50)
	 *
	 * @return Model name.
	 */
	public abstract String modelPrettyName();

	public abstract void setVariation(Enum variation);

	@Override public Class<? extends Model> modelType() {
		return ComputationGraph.class;
	}

	@Override public ComputationGraph init() {
		// We initialize the model in initPretrained()
		return null;
	}

	public String pretrainedUrl(PretrainedType pretrainedType) {
		if (pretrainedType == PretrainedType.IMAGENET) {
			return KerasConstants.Locations.get(modelPrettyName());
		} else {
			return null;
		}
	}

	public long pretrainedChecksum(PretrainedType pretrainedType) {
		if (pretrainedType == PretrainedType.IMAGENET) {
			return KerasConstants.Checksums.get(modelPrettyName());
		} else {
			return 0L;
		}
	}

	@Override public ComputationGraph initPretrained(PretrainedType pretrainedType) throws IOException {
		String remoteUrl = pretrainedUrl(pretrainedType);
		if (remoteUrl == null)
			throw new UnsupportedOperationException(
					"Pretrained " + pretrainedType + " weights are not available for this model.");

		// Set up file locations
		String localFilename = modelPrettyName() + ".zip";

		File rootCacheDir = DL4JResources.getDirectory(ResourceType.ZOO_MODEL, modelFamily());
		File cachedFile = new File(rootCacheDir, localFilename);

		// Download the file if necessary
		if (!cachedFile.exists()) {
			log.info("Downloading model to " + cachedFile.toString());
			FileUtils.copyURLToFile(new URL(remoteUrl), cachedFile);
		} else {
			log.info("Using cached model at " + cachedFile.toString());
		}

		// Validate the checksum - ensure this is the correct file
		long expectedChecksum = pretrainedChecksum(pretrainedType);
		if (expectedChecksum != 0L) {
			log.info("Verifying download...");
			Checksum adler = new Adler32();
			FileUtils.checksum(cachedFile, adler);
			long localChecksum = adler.getValue();
			log.info("Checksum local is " + localChecksum + ", expecting " + expectedChecksum);

			if (expectedChecksum != localChecksum) {
				log.error("Checksums do not match. Cleaning up files and failing...");
				cachedFile.delete();
				throw new IllegalStateException(
						String.format("Pretrained model file for model %s failed checksum.", this.modelPrettyName()));
			}
		}

		// Load the .zip file to a ComputationGraph
		try {
			return ModelSerializer.restoreComputationGraph(cachedFile);
		} catch (Exception ex) {
			log.error("Failed to load model");
			ex.printStackTrace();
			return null;
		}
	}

	@Override public void setInputShape(int[][] inputShape) {
		this.inputShape = inputShape[0];
	}

	@Override public ModelMetaData metaData() {
		return null;
	}
}
