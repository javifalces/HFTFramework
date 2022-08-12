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
 * SubsamplingLayer.java
 * Copyright (C) 2016-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.dl4j.layers;

import java.io.Serializable;
import java.util.Enumeration;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.OptionMetadata;
import weka.dl4j.enums.ConvolutionMode;
import weka.dl4j.enums.PoolingType;
import weka.gui.ProgrammaticProperty;

/**
 * A version of DeepLearning4j's SubsamplingLayer that implements WEKA option handling.
 *
 * @author Christopher Beckham
 * @author Eibe Frank
 * @author Steven Lang
 */
public class SubsamplingLayer extends Layer<org.deeplearning4j.nn.conf.layers.SubsamplingLayer>
		implements OptionHandler, Serializable {

	/**
	 * The ID used to serialize this class.
	 */
	private static final long serialVersionUID = -699034028619492301L;

	/**
	 * Constructor for setting some defaults.
	 */
	public SubsamplingLayer() {
		super();
		setLayerName("Subsampling layer");
		setConvolutionMode(ConvolutionMode.Truncate);
		setKernelSize(new int[] { 2, 2 });
		setStride(new int[] { 1, 1 });
		setPadding(new int[] { 0, 0 });
		setPoolingType(PoolingType.MAX);
		setEps(1e-8);
		setPnorm(1);
	}

	@Override public void initializeBackend() {
		backend = new org.deeplearning4j.nn.conf.layers.SubsamplingLayer();
	}

	/**
	 * Global info.
	 *
	 * @return string describing this class.
	 */
	public String globalInfo() {
		return "A subsampling layer from DeepLearning4J.";
	}

	@OptionMetadata(displayName = "eps", description = "The value of the eps parameter (default = 1e-8).", commandLineParamName = "eps", commandLineParamSynopsis = "-eps <double>", displayOrder = 2) public double getEps() {
		return backend.getEps();
	}

	public void setEps(double e) {
		backend.setEps(e);
	}

	@OptionMetadata(displayName = "pnorm", description = "The value of the pnorm parameter (default = 1).", commandLineParamName = "pnorm", commandLineParamSynopsis = "-pnorm <int>", displayOrder = 3) public int getPnorm() {
		return backend.getPnorm();
	}

	public void setPnorm(int p) {
		backend.setPnorm(p);
	}

	@OptionMetadata(displayName = "convolution mode", description = "The convolution mode (default = Truncate).", commandLineParamName = "mode", commandLineParamSynopsis = "-mode <string>", displayOrder = 2) public ConvolutionMode getConvolutionMode() {
		return ConvolutionMode.fromBackend(backend.getConvolutionMode());
	}

	public void setConvolutionMode(ConvolutionMode convolutionMode) {
		backend.setConvolutionMode(convolutionMode.getBackend());
	}

	@OptionMetadata(displayName = "number of rows in kernel", description = "The number of rows in the kernel (default = 5).", commandLineParamName = "rows", commandLineParamSynopsis = "-rows <int>", displayOrder = 4) public int getKernelSizeX() {
		return backend.getKernelSize()[0];
	}

	public void setKernelSizeX(int kernelSizeX) {
		int[] kernelSize = new int[] { kernelSizeX, getKernelSizeY() };
		backend.setKernelSize(kernelSize);
	}

	@OptionMetadata(displayName = "number of columns in kernel", description = "The number of columns in the kernel (default = 5).", commandLineParamName = "columns", commandLineParamSynopsis = "-columns <int>", displayOrder = 5) public int getKernelSizeY() {
		return backend.getKernelSize()[1];
	}

	public void setKernelSizeY(int kernelSizeY) {
		int[] kernelSize = new int[] { getKernelSizeX(), kernelSizeY };
		backend.setKernelSize(kernelSize);
	}

	@ProgrammaticProperty public int[] getKernelSize() {
		return backend.getKernelSize();
	}

	public void setKernelSize(int[] kernelSize) {
		backend.setKernelSize(kernelSize);
	}

	@OptionMetadata(displayName = "number of rows in stride", description = "The stride along the rows (default = 1).", commandLineParamName = "strideRows", commandLineParamSynopsis = "-strideRows <int>", displayOrder = 6) public int getStrideRows() {
		return backend.getStride()[0];
	}

	public void setStrideRows(int rows) {
		int[] stride = new int[] { rows, getStrideColumns() };
		backend.setStride(stride);
	}

	@ProgrammaticProperty public int[] getStride() {
		return backend.getStride();
	}

	public void setStride(int[] stride) {
		backend.setStride(stride);
	}

	@OptionMetadata(displayName = "number of columns in stride", description = "The stride along the columns (default = 1).", commandLineParamName = "strideColumns", commandLineParamSynopsis = "-strideColumns <int>", displayOrder = 7) public int getStrideColumns() {
		return backend.getStride()[1];
	}

	public void setStrideColumns(int columns) {
		int[] stride = new int[] { getStrideRows(), columns };
		backend.setStride(stride);
	}

	@OptionMetadata(displayName = "number of rows in padding", description = "The number of rows in the padding (default = 0).", commandLineParamName = "paddingRows", commandLineParamSynopsis = "-paddingRows <int>", displayOrder = 8) public int getPaddingRows() {
		return backend.getPadding()[0];
	}

	public void setPaddingRows(int padding) {
		int[] pad = new int[] { padding, getPaddingColumns() };
		backend.setPadding(pad);
	}

	@ProgrammaticProperty public int[] getPadding() {
		return backend.getPadding();
	}

	public void setPadding(int[] padding) {
		backend.setPadding(padding);
	}

	@OptionMetadata(displayName = "number of columns in padding", description = "The number of columns in the padding (default = 0).", commandLineParamName = "paddingColumns", commandLineParamSynopsis = "-paddingColumns <int>", displayOrder = 9) public int getPaddingColumns() {
		return backend.getPadding()[1];
	}

	public void setPaddingColumns(int padding) {
		int[] pad = new int[] { getPaddingRows(), padding };
		backend.setPadding(pad);
	}

	@OptionMetadata(displayName = "pooling type", description = "The type of pooling to use (default = MAX; options: MAX, AVG, SUM, NONE).", commandLineParamName = "poolingType", commandLineParamSynopsis = "-poolingType <string>", displayOrder = 10) public PoolingType getPoolingType() {
		return PoolingType.fromBackend(backend.getPoolingType());
	}

	public void setPoolingType(PoolingType poolingType) {
		backend.setPoolingType(poolingType.getBackend());
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options.
	 */
	@Override public Enumeration<Option> listOptions() {
		return Option.listOptionsForClassHierarchy(this.getClass(), super.getClass()).elements();
	}

	/**
	 * Gets the current settings of the Classifier.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override public String[] getOptions() {
		return Option.getOptionsForHierarchy(this, super.getClass());
	}

	/**
	 * Parses a given list of options.
	 *
	 * @param options the list of options as an array of strings
	 * @throws Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {
		Option.setOptionsForHierarchy(options, this, super.getClass());
	}
}
