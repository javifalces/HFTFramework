package weka.gui.explorer;

import weka.dl4j.inference.PredictionClass;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Class selector panel.
 */
public class ClassSelector {

	/**
	 * Class map to use for displaying the given class name.
	 */
	private String[] classMap;

	/**
	 * Regex supplied by the user.
	 */
	String m_PatternRegEx = "";

	/**
	 * Pointer to the parent panel in the GUI.
	 */
	private JPanel parentPanel;

	/**
	 * Label for the target class ID.
	 */
	JLabel targetClassIDLabel = new JLabel("Target Class ID:");
	/**
	 * Input for the target class.
	 */
	JTextField targetClassIDInput = new JTextField();
	/**
	 * Label for the class name input.
	 */
	JLabel classNameLabel = new JLabel("  Class Name:");
	/**
	 * Input for the class name.
	 */
	JTextField classNameInput = new JTextField();
	/**
	 * Button to apply a regex pattern.
	 */
	JButton patternButton = new JButton("Pattern");

	/**
	 * Init the ClassSelector panel.
	 *
	 * @param parentPanel Parent panel in the GUI.
	 * @param classMap    Class map to display class names from.
	 */
	public ClassSelector(JPanel parentPanel, String[] classMap) {
		this.parentPanel = parentPanel;
		this.classMap = classMap;
	}

	/**
	 * Init the GUI elements.
	 *
	 * @param defaultClassID Default ID to use.
	 * @param rowNum         Row on the parent GUI to add ourselves to.
	 */
	public void initOnGUI(int defaultClassID, int rowNum) {
		setup(rowNum);
		setTargetClass(defaultClassID);
	}

	/**
	 * Setup the GUI elements.
	 *
	 * @param rowNum Row number on the parent panel to add ourselves to.
	 */
	private void setup(int rowNum) {
		patternButton.addActionListener(e -> openPatternDialog());

		// Setup the button listeners
		targetClassIDInput.getDocument().addDocumentListener(new DocumentListener() {

			@Override public void insertUpdate(DocumentEvent e) {
				updateClassNameInput();
			}

			@Override public void removeUpdate(DocumentEvent e) {
				updateClassNameInput();
			}

			@Override public void changedUpdate(DocumentEvent e) {
				updateClassNameInput();
			}
		});

		// Define the UI elements
		targetClassIDInput.setColumns(5);
		targetClassIDInput.setToolTipText("-1 to use max probability class");
		classNameInput.setEditable(false);

		GridBagConstraints gbc = new SaliencyMapWindow.SaliencyMapGBC();

		gbc.gridy = rowNum;

		gbc.gridx = 0;
		parentPanel.add(targetClassIDLabel, gbc);

		gbc.gridx = 1;
		targetClassIDInput.setMinimumSize(new Dimension(50, 28));
		parentPanel.add(targetClassIDInput, gbc);

		gbc.gridx = 2;
		parentPanel.add(classNameLabel, gbc);

		gbc.gridx = 3;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		parentPanel.add(classNameInput, gbc);

		gbc.gridx = 4;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		parentPanel.add(patternButton, gbc);
	}

	/**
	 * Remove ourselves from the parent panel.
	 */
	public void removeFromParent() {
		parentPanel.remove(targetClassIDLabel);
		parentPanel.remove(targetClassIDInput);
		parentPanel.remove(classNameLabel);
		parentPanel.remove(classNameInput);
		parentPanel.remove(patternButton);
	}

	public void setTargetClass(int id) {
		targetClassIDInput.setText("" + id);
	}

	public int getTargetClass() {
		return Integer.parseInt(targetClassIDInput.getText());
	}

	/**
	 * Open the classmap regex pattern matching dialog. Set the target class if the user completes it.
	 */
	private void openPatternDialog() {
		String pattern = JOptionPane
				.showInputDialog(patternButton.getParent(), "Enter a Perl regular expression", m_PatternRegEx);
		if (pattern != null) {
			try {
				Pattern.compile(pattern);
				m_PatternRegEx = pattern;
				ArrayList<PredictionClass> matchingClasses = getMatchingClasses(pattern);

				if (matchingClasses.isEmpty()) {
					JOptionPane.showMessageDialog(null, "No classes matched that regex pattern");
					return;
				} else if (matchingClasses.size() == 1) {
					setTargetClass(matchingClasses.get(0).getClassID());
				} else {
					PredictionClass selectedClass = selectOneOfNClasses(matchingClasses);
					setTargetClass(selectedClass.getClassID());
				}

			} catch (Exception ex) {
				JOptionPane.showMessageDialog(patternButton.getParent(),
						"'" + pattern + "' is not a valid Perl regular expression!\n" + "Error: " + ex,
						"Error in Pattern...", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * A final dialog box to prompt the user to select of a set of classes, if their regex
	 * matches multiple classes.
	 *
	 * @param matchingClasses List of classes the original regex matched.
	 * @return The selected PredictionClass.
	 */
	private PredictionClass selectOneOfNClasses(ArrayList<PredictionClass> matchingClasses) {
		return (PredictionClass) JOptionPane
				.showInputDialog(null, "The pattern matched multiple classes, please select one", "Select a class",
						JOptionPane.QUESTION_MESSAGE, null, matchingClasses.toArray(), // Array of choices
						matchingClasses.get(0));
	}

	/**
	 * Get the list of classes in our classmap that matches the supplied pattern.
	 *
	 * @param pattern Pattern to match against.
	 * @return List of matching classes.
	 */
	public ArrayList<PredictionClass> getMatchingClasses(String pattern) {
		ArrayList<PredictionClass> result = new ArrayList<>();
		for (int i = 0; i < classMap.length; i++) {
			String tmpClass = classMap[i];
			if (Pattern.matches(pattern, tmpClass)) {
				result.add(new PredictionClass(i, tmpClass));
			}
		}
		return result;
	}

	/**
	 * Update  the class name input with the corresponding class name to our input class ID.
	 * Doesn't update if the class ID is invalid.
	 */
	private void updateClassNameInput() {
		String targetClassIDText = targetClassIDInput.getText();
		if (targetClassIDText.isEmpty()) {
			return;
		}
		if (this.classMap.length == 0) {
			return;
		}
		int classID;
		try {
			classID = Integer.parseInt(targetClassIDText);
		} catch (NumberFormatException ex) {
			// Don't update the class name if the target class isn't a number
			return;
		}

		// Don't update the class name if the class ID is outside the bounds of the class map
		if (classID > classMap.length - 1 || classID < 0)
			return;

		// Update the class name in the GUI
		String newClassName = classMap[classID];
		classNameInput.setText(newClassName);
	}
}
