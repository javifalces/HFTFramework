package weka.core.progress;

import javax.swing.*;
import java.awt.*;

/**
 * Progress bar panel displayed in the GUI.
 */
public class GUIProgressBar extends AbstractProgressBar implements Runnable {

	/**
	 * The main JFrame.
	 */
	protected JFrame currentFrame;

	/**
	 * The main progress bar.
	 */
	protected JProgressBar progressBar;

	/**
	 * Displays the user supplied progress message.
	 */
	protected JLabel progressMessageLabel;

	/**
	 * Displays the calculated ETA.
	 */
	protected JLabel etaLabel;

	/**
	 * Init the GUI progress bar
	 *
	 * @param maxProgress     Max progress for the bar
	 * @param progressMessage Message to display
	 */
	public GUIProgressBar(double maxProgress, String progressMessage) {
		super(maxProgress, progressMessage);
		initGUIElements();
	}

	/**
	 * Initialize the GUI elements on the frame.
	 */
	private void initGUIElements() {
		// Create frame with title Registration Demo
		currentFrame = new JFrame();
		currentFrame.setTitle("Weka Progress Meter");

		progressMessageLabel = new JLabel(getProgressMessage());

		etaLabel = new JLabel(getETAString());

		progressBar = new JProgressBar();
		progressBar.setIndeterminate(m_indeterminate);
		progressBar.setPreferredSize(new Dimension(400, 30));
		progressBar.setMaximum((int) getMaxProgress());

		// Panel to define the layout. We are using GridBagLayout
		JPanel mainPanel = new JPanel();
		GridBagLayout gbL = new GridBagLayout();
		mainPanel.setLayout(gbL);
		mainPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Progress"),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		GridBagConstraints gbC = new GridBagConstraints();
		gbC.anchor = GridBagConstraints.CENTER;
		gbC.gridx = 0;
		gbC.gridy = 0;
		gbL.setConstraints(progressMessageLabel, gbC);
		mainPanel.add(progressMessageLabel);

		gbC = new GridBagConstraints();
		gbC.anchor = GridBagConstraints.CENTER;
		gbC.gridy = 1;
		gbC.gridx = 0;
		gbC.insets = new Insets(0, 0, 20, 0);
		gbL.setConstraints(etaLabel, gbC);
		mainPanel.add(etaLabel);

		gbC = new GridBagConstraints();
		gbC.anchor = GridBagConstraints.CENTER;
		gbC.gridy = 2;
		gbC.gridx = 0;
		gbL.setConstraints(progressBar, gbC);
		mainPanel.add(progressBar);

		// Add panel to frame
		currentFrame.add(mainPanel);
	}

	/**
	 * Update the progress bar with the current value.
	 */
	@Override protected void onSetProgress() {
		progressBar.setValue((int) m_actualProgress);
	}

	/**
	 * Do any required setup on start (e.g., show a popup window)
	 */
	@Override protected void onStart() {
		currentFrame.pack();
		currentFrame.setLocationRelativeTo(null);
		currentFrame.setVisible(true);
	}

	/**
	 * Perform any teardown (e.g., close popup window, display completion message)
	 */
	@Override public void finish() {
		if (currentFrame != null) {
			currentFrame.dispose();
			currentFrame = null;
		}
	}

	/**
	 * Update the progress bar display.
	 */
	@Override public void refreshDisplay() {
		SwingUtilities.invokeLater(() -> {
			etaLabel.setText(getETAString());
			currentFrame.pack();
		});
	}

	@Override public void run() {
		start();
	}
}
