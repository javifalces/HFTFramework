package weka.gui.explorer;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import weka.core.WekaException;
import weka.core.progress.ProgressManager;
import weka.dl4j.ResourceResolver;
import weka.dl4j.inference.Dl4jCNNExplorer;
import weka.dl4j.interpretability.AbstractCNNSaliencyMapWrapper;
import weka.gui.ExtensionFileFilter;
import weka.gui.WekaFileChooser;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * JPanel showing the saliency map generated, as well as options for configuring it.
 */
@Log4j2 public class SaliencyMapWindow extends JPanel {

	/**
	 * Wrapper class for GBC which defaults to inset size of 5.
	 */
	public static class SaliencyMapGBC extends GridBagConstraints {

		/**
		 * Init.
		 */
		public SaliencyMapGBC() {
			this(5);
		}

		/**
		 * Init.
		 *
		 * @param insetSize Inset size for objects
		 */
		public SaliencyMapGBC(int insetSize) {
			this.insets = new Insets(insetSize, insetSize, insetSize, insetSize);
		}
	}

	/**
	 * The Dl4jCNNExplorer which has processed the image.
	 */
	Dl4jCNNExplorer processedExplorer;

	/**
	 * UI Elements.
	 */
	JFrame thisWindow = new JFrame("WekaDeeplearning4j - Saliency Map Viewer");

	/**
	 * Displays the saliency image.
	 */
	JLabel saliencyImageLabel;

	/**
	 * Displays the saliency image.
	 */
	ImageIcon icon;
	/**
	 * Stores the saliency image.
	 */
	Image saliencyImage;
	/**
	 * Checkbox for the user to normalize the heatmap.
	 */
	JCheckBox normalizeHeatmapCheckbox = new JCheckBox("Normalize heatmap");
	/**
	 * Add class button.
	 */
	JButton addClassButton = new JButton("Add Class");
	/**
	 * Remove class button.
	 */
	JButton removeClassButton = new JButton("Remove Class");
	/**
	 * Generate a heatmap button.
	 */
	JButton generateButton = new JButton("Generate");
	/**
	 * Save the heatmap as an image button.
	 */
	JButton saveHeatmapButton = new JButton("Save...");
	/**
	 * Scrollpane for the image, allowing us to generate for many more classes than we can fit on screen.
	 */
	JScrollPane scrollPane;
	/**
	 * Panel for the buttons above.
	 */
	JPanel buttonPanel;
	/**
	 * Control panel.
	 */
	JPanel controlPanel;
	/**
	 * Split pane to allow us to hide the control buttons.
	 */
	JSplitPane splitPane;

	/**
	 * The filepath for the default saliency map image.
	 */
	private String DEFAULT_SALIENCY_IMAGE_PATH;

	/**
	 * Filter for the save dialog.
	 */
	protected FileFilter m_ImageFilter = new ExtensionFileFilter(ExplorerDl4jInference.IMAGE_FILE_EXTENSIONS,
			"Image files");

	/**
	 * The file chooser for saving the image.
	 */
	protected WekaFileChooser m_FileChooser = new WekaFileChooser(new File(System.getProperty("user.dir")));

	/**
	 * List of all the ClassSelectors currently open.
	 */
	ArrayList<ClassSelector> classSelectors = new ArrayList<>();

	/**
	 * Which GBC row should the buttons appear on.
	 */
	int buttonsRow = 0;
	/**
	 * Which GBC row should the target class fields start from.
	 */
	int targetClassRow = 1;
	/**
	 * Which row should the image label appear on (bottom).
	 */
	int imageRow = 20;

	/**
	 * Initialize the window.
	 */
	public SaliencyMapWindow() {
		try {
			DEFAULT_SALIENCY_IMAGE_PATH = new ResourceResolver().GetResolvedPath("placeholderSaliencyMap.png");
		} catch (WekaException ex) {
			ex.printStackTrace();
		}

		oneTimeSetup();
	}

	/**
	 * Add a new class selector to the panel.
	 */
	private void addClassSelector() {
		if (classSelectors.size() == 5) {
			// Limit the size to 10
			return;
		}
		ClassSelector classSelector = new ClassSelector(controlPanel, getCurrentClassMap());
		classSelector.initOnGUI(getDefaultClassID(), targetClassRow + classSelectors.size());
		classSelectors.add(classSelector);
		packWindow();
	}

	/**
	 * Clear all class selectors from the panel.
	 */
	private void clearClassSelectors() {
		while (classSelectors.size() > 0) {
			removeClassSelector();
		}
	}

	/**
	 * Remove the last class selector.
	 */
	private void removeClassSelector() {
		if (classSelectors.size() == 0) {
			// Don't go below one class selector
			return;
		}
		ClassSelector lastClassSelector = classSelectors.get(classSelectors.size() - 1);
		lastClassSelector.removeFromParent();
		classSelectors.remove(lastClassSelector);
		packWindow();
	}

	/**
	 * This resets the window size to fit all the components inside it.
	 */
	private void packWindow() {
		//        var originalDimension = thisWindow.getSize();
		thisWindow.pack();
		splitPane.setDividerLocation(splitPane.getMinimumDividerLocation());
		//        thisWindow.setSize(originalDimension);
	}

	/**
	 * Setup the listeners for the panel buttons.
	 */
	private void setupButtonListeners() {
		generateButton.addActionListener(e -> generateSaliencyMap());
		saveHeatmapButton.addActionListener(e -> saveHeatmap());
		normalizeHeatmapCheckbox.addActionListener(e -> generateSaliencyMap());
		addClassButton.addActionListener(e -> addClassSelector());
		removeClassButton.addActionListener(e -> removeClassSelector());

		// Set the default class ID in the window
		normalizeHeatmapCheckbox.setSelected(true);
	}

	/**
	 * Add the buttons to the control panel.
	 */
	private void addControlButtons() {
		GridBagConstraints gbc = new SaliencyMapGBC();
		gbc.gridy = buttonsRow;

		gbc.gridx = 0;
		gbc.gridwidth = 5;
		buttonPanel = new JPanel(new GridLayout(1, 5, 30, 5));
		buttonPanel.add(addClassButton);
		buttonPanel.add(removeClassButton);
		buttonPanel.add(normalizeHeatmapCheckbox);
		buttonPanel.add(generateButton);
		buttonPanel.add(saveHeatmapButton);
		controlPanel.add(buttonPanel, gbc);
	}

	/**
	 * Add the scrollable image to the main panel.
	 */
	private void addScrollableImage() {
		GridBagConstraints gbc = new SaliencyMapGBC();
		gbc.gridx = 0;
		gbc.gridy = imageRow;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 5;
		gbc.gridheight = 1;
		saliencyImageLabel = new JLabel(new ImageIcon(DEFAULT_SALIENCY_IMAGE_PATH));
		scrollPane = new JScrollPane(saliencyImageLabel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
	}

	/**
	 * Onetime setup for the window - listeners, etc.
	 */
	private void oneTimeSetup() {
		setupButtonListeners();

		setLayout(new BorderLayout());
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Saliency Map Viewer"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		controlPanel = new JPanel(new GridBagLayout());

		addControlButtons();

		addScrollableImage();

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, controlPanel, scrollPane);
		splitPane.setOneTouchExpandable(true);

		add(splitPane, BorderLayout.CENTER);
	}

	public String[] getCurrentClassMap() {
		if (processedExplorer == null) {
			return new String[0];
		} else {
			return processedExplorer.getModelOutputDecoder().getClasses();
		}
	}

	/**
	 * Show the saliency map.
	 *
	 * @param explorer Processed explorer
	 */
	public void open(Dl4jCNNExplorer explorer) {
		this.processedExplorer = explorer;

		// Start with a fresh class selector
		clearClassSelectors();
		addClassSelector();

		thisWindow.add(this);
		packWindow();
		thisWindow.setLocationRelativeTo(null);
		thisWindow.setVisible(true);

		generateSaliencyMap();
	}

	public int getDefaultClassID() {
		if (processedExplorer != null)
			return processedExplorer.getCurrentPredictions().getTopPrediction().getClassID();
		else
			return -1;
	}

	private int[] getTargetClassIDs() {
		return classSelectors.stream().mapToInt(ClassSelector::getTargetClass).toArray();
	}

	/**
	 * Check that all supplied class IDs are valid.
	 */
	private void validateClassID() {
		Integer[] targetClasses = classSelectors.stream().map(ClassSelector::getTargetClass).toArray(Integer[]::new);
		log.info("Generating for classes = " + Arrays.toString(targetClasses));

		for (Integer targetClass : targetClasses) {
			if (targetClass < 0 || targetClass >= getCurrentClassMap().length) {
				throw new IllegalArgumentException("Invalid class ID(s) supplied: " + Arrays.toString(targetClasses));
			}
		}
	}

	/**
	 * Generate the saliency map then display it in the window.
	 */
	private void generateSaliencyMap() {
		validateClassID();
		SwingWorker<Image, Void> worker = new SwingWorker<Image, Void>() {

			@Override protected Image doInBackground() {
				ProgressManager manager = new ProgressManager(-1, "Generating saliency map...");
				manager.start();
				boolean normalize = normalizeHeatmapCheckbox.isSelected();

				AbstractCNNSaliencyMapWrapper wrapper = processedExplorer.getSaliencyMapWrapper();
				wrapper.setTargetClassIDsAsInt(getTargetClassIDs());
				wrapper.setNormalizeHeatmap(normalize);

				processedExplorer.setSaliencyMapWrapper(wrapper);
				Image outputMap = processedExplorer.generateOutputMap();
				manager.finish();

				return outputMap;
			}

			@SneakyThrows @Override protected void done() {
				super.done();
				setSaliencyImage(get());

				// Pack
				packWindow();
			}
		};
		worker.execute();
	}

	private void setSaliencyImage(Image image) {
		if (image == null) {
			try {
				image = ImageIO.read(new File(DEFAULT_SALIENCY_IMAGE_PATH));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		saliencyImage = image;
		icon = new ImageIcon(saliencyImage);
		saliencyImageLabel.setIcon(icon);
		saliencyImageLabel.invalidate();
	}

	/**
	 * Show the file chooser and save the heatmap to the user specified location.
	 */
	private void saveHeatmap() {
		// Prompt the user for a place to save the image to
		m_FileChooser.setFileFilter(m_ImageFilter);

		int returnCode = m_FileChooser.showSaveDialog(this);

		if (returnCode == 1) {
			log.error("User did not select a new image");
			return;
		}
		// Get the image
		File outputFile = m_FileChooser.getSelectedFile();

		String extension = FilenameUtils.getExtension(outputFile.getName());

		try {
			ImageIO.write((RenderedImage) saliencyImage, extension, outputFile);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
