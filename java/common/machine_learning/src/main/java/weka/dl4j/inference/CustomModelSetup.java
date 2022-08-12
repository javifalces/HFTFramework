package weka.dl4j.inference;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.Utils;
import weka.dl4j.zoo.AbstractZooModel;

import java.io.File;
import java.io.Serializable;
import java.util.Enumeration;

/**
 * Config class to hold parameters for a custom model that was trained previously.
 */
public class CustomModelSetup implements Serializable, OptionHandler {

	/**
	 * The classifier model this filter is based on.
	 */
	protected File serializedModelFile = new File(Utils.defaultFileLocation());

	/**
	 * Input channels for the model.
	 */
	protected int inputChannels = 3;

	/**
	 * Input width for the model.
	 */
	protected int inputWidth = 224;

	/**
	 * Input height for the model.
	 */
	protected int inputHeight = 224;

	public void setUseCustomSetup(boolean useCustomSetup) {
		if (!useCustomSetup) {
			resetModelFilepath();
		}
	}

	/**
	 * Reset the model filepath to the default.
	 */
	public void resetModelFilepath() {
		setSerializedModelFile(new File(Utils.defaultFileLocation()));
	}

	@OptionMetadata(displayName = "Serialized model file", description = "Pointer to file of saved Dl4jMlpClassifier", commandLineParamName = "model-file", commandLineParamSynopsis = "-model-file <file path>", displayOrder = 0) public File getSerializedModelFile() {
		return serializedModelFile;
	}

	public void setSerializedModelFile(File serializedModelFile) {
		this.serializedModelFile = serializedModelFile;
	}

	@OptionMetadata(displayName = "Number of input channels", description = "Number of channels for input images to this model", commandLineParamName = "channels", commandLineParamSynopsis = "-channels <number>", displayOrder = 1) public int getInputChannels() {
		return inputChannels;
	}

	public void setInputChannels(int inputChannels) {
		this.inputChannels = inputChannels;
	}

	@OptionMetadata(displayName = "Input image width", description = "Width of input images to this model", commandLineParamName = "width", commandLineParamSynopsis = "-width <number>", displayOrder = 2) public int getInputWidth() {
		return inputWidth;
	}

	public void setInputWidth(int inputWidth) {
		this.inputWidth = inputWidth;
	}

	@OptionMetadata(displayName = "Input image height", description = "Height of input images to this model", commandLineParamName = "height", commandLineParamSynopsis = "-height <number>", displayOrder = 3) public int getInputHeight() {
		return inputHeight;
	}

	public void setInputHeight(int inputHeight) {
		this.inputHeight = inputHeight;
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	public Enumeration<Option> listOptions() {
		return Option.listOptionsForClassHierarchy(this.getClass(), AbstractZooModel.class).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {
		return Option.getOptionsForHierarchy(this, AbstractZooModel.class);
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		Option.setOptionsForHierarchy(options, this, AbstractZooModel.class);

		weka.core.Utils.checkForRemainingOptions(options);
	}
}
