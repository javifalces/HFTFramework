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
 * Word2VecLoader.java
 * Copyright (C) 2017-2018 University of Waikato, Hamilton, New Zealand
 */

package weka.core.converters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Loads Word2Vec seriliazed embeddings into Weka.
 *
 * @author Felipe Bravo-Marquez
 */
public class Word2VecLoader extends AbstractFileLoader implements BatchConverter {

	/**
	 * For serialization
	 */
	private static final long serialVersionUID = -5963779116425129124L;

	/**
	 * the file extension.
	 */
	public static String FILE_EXTENSION = ".bin";

	/**
	 * the extension for compressed files.
	 */
	public static String FILE_EXTENSION_COMPRESSED = FILE_EXTENSION + ".gz";

	/**
	 * Word2Vec object
	 */
	private Word2Vec vec;

	/**
	 * Main method for testing this class.
	 *
	 * @param args should contain &lt;filestem&gt;[.names | data]
	 */
	public static void main(String[] args) {
		runFileLoader(new Word2VecLoader(), args);
	}

	/* (non-Javadoc)
	 * @see
	 */

	/**
	 * @return Word2Vec file extension
	 * @see FileSourcedConverter#getFileExtension()
	 */
	@Override public String getFileExtension() {
		return FILE_EXTENSION;
	}

	@Override
	/*
	 * Gets all the file extensions used for this type of file.
	 *
	 * @return the file extensions
	 */ public String[] getFileExtensions() {
		return new String[] { FILE_EXTENSION, FILE_EXTENSION_COMPRESSED };
	}

	/* (non-Javadoc)
	 * @see weka.core.converters.FileSourcedConverter#getFileDescription()
	 */
	@Override public String getFileDescription() {
		return "W2V binary word embeddings.";
	}

	/* (non-Javadoc)
	 * @see weka.core.RevisionHandler#getRevision()
	 */
	@Override public String getRevision() {
		return "$Revision: 1 $";
	}

	/**/
	public void setStructure() {
		ArrayList<Attribute> att = new ArrayList<Attribute>();

		// Add one attribute for each embedding dimension
		for (int i = 0; i < this.vec.getLayerSize(); i++) {
			att.add(new Attribute("embedding-" + i));
		}

		att.add(new Attribute("word_id", (ArrayList<String>) null));

		m_structure = new Instances("W2V model loaded from " + this.m_File.toString(), att, 0);
	}

	@Override public Instances getStructure() throws IOException {
		if (m_sourceFile == null) {
			throw new IOException("No source has been specified.");
		}

		if (m_structure == null) {
			setSource(m_sourceFile);
			this.vec = WordVectorSerializer.readWord2VecModel(m_sourceFile);
			this.setStructure();
		}

		return m_structure;
	}

	@Override public Instances getDataSet() throws IOException {
		if (m_sourceFile == null) {
			throw new IOException("No source has been specified");
		}

		if (getRetrieval() == INCREMENTAL) {
			throw new IOException("This loader cannot load instances incrementally.");
		}
		setRetrieval(BATCH);

		if (m_structure == null) {
			getStructure();
		}

		Instances result = new Instances(m_structure);

		for (String word : vec.getVocab().words()) {
			double[] values = new double[result.numAttributes()];

			for (int i = 0; i < this.vec.getWordVector(word).length; i++) {
				values[i] = this.vec.getWordVector(word)[i];
			}

			values[result.numAttributes() - 1] = result.attribute("word_id").addStringValue(word);

			Instance inst = new DenseInstance(1, values);

			inst.setDataset(result);

			result.add(inst);
		}

		return result;
	}

	/**
	 * Resets the Loader object and sets the source of the data set to be the supplied File object.
	 *
	 * @param file the source file.
	 * @throws IOException if an error occurs
	 */
	public void setSource(File file) throws IOException {
		m_structure = null;

		setRetrieval(NONE);

		if (file == null) {
			throw new IOException("Source file object is null!");
		}

		m_sourceFile = file;
		m_File = file.getAbsolutePath();
	}

	/* (non-Javadoc)
	 * @see weka.core.converters.AbstractLoader#getNextInstance(weka.core.Instances)
	 */
	@Override public Instance getNextInstance(Instances structure) throws IOException {
		throw new IOException("This loader cannot load data incrementally.");
	}
}
