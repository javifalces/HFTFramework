package weka.core.progress;

import lombok.extern.log4j.Log4j2;

import java.io.Serializable;

/**
 * Main entrypoint for any progress-bar related tasks.
 * Handles switching between GUI and command line, based on whether
 * WEKA is being run from GUI or command line (or Java code)
 */
@Log4j2 public class ProgressManager implements Serializable {

	/**
	 * Progress bar to control.
	 */
	protected AbstractProgressBar progressBar;

	/**
	 * Flag indicating if we're running the GUI.
	 */
	protected boolean runningInGUI = false;

	/**
	 * Init a new progress manager, setting the maximum progress and progress message.
	 *
	 * @param maxProgress     Maximum progress value (e.g., max number of iterations)
	 * @param progressMessage Progress message to display
	 */
	public ProgressManager(double maxProgress, String progressMessage) {
		init(maxProgress, progressMessage);
	}

	/**
	 * Init a new indeterminate progress manager (no notion of iterating progress).
	 *
	 * @param indeterminateProgressMessage Progress message to display
	 */
	public ProgressManager(String indeterminateProgressMessage) {
		init(-1, indeterminateProgressMessage);
	}

	/**
	 * Init the progress manager.
	 */
	public ProgressManager() {
		init(-1, "");
	}

	/**
	 * Checks the stacktrace for a call to anything in the weka.gui package.
	 * If that exists, WEKA was started by the GUIChooser
	 *
	 * @return True if being run within the GUI.
	 */
	public boolean checkIfRunByGUI() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		boolean tmpRunningInGUI = false;
		for (StackTraceElement s : stack) {
			if (s.getClassName().contains("weka.gui.")) {
				tmpRunningInGUI = true;
				log.debug("Weka started by GUIChooser");
				break;
			}
		}
		return tmpRunningInGUI;
	}

	/**
	 * Initializes either a GUI or command line progress bar.
	 *
	 * @param maxProgress     Maximum progress value
	 * @param progressMessage Progress message to display
	 */
	private void init(double maxProgress, String progressMessage) {
		runningInGUI = checkIfRunByGUI();

		if (runningInGUI) {
			progressBar = new GUIProgressBar(maxProgress, progressMessage);
		} else {
			// Create the text progress bar as we're running it from code/command line
			progressBar = new CommandLineProgressBar(maxProgress, progressMessage);
		}

		if (maxProgress == -1) {
			progressBar.setIndeterminate(true);
		}
	}

	/* Progress Bar passthrough methods */

	/**
	 * Show the progress bar and start the timer.
	 */
	public void start() {
		progressBar.start();
	}

	/**
	 * Increment the progress bar.
	 */
	public void increment() {
		progressBar.increment();
	}

	/**
	 * Set the progress to a specific value
	 *
	 * @param progress Current progress
	 */
	public void setProgress(double progress) {
		progressBar.setProgress(progress);
	}

	/**
	 * Close out the progress bar.
	 */
	public void finish() {
		progressBar.finish();
	}

	/**
	 * Set the maximum progress value of the progress bar
	 *
	 * @param maxProgress Maximum progress value
	 */
	public void setMaxProgress(double maxProgress) {
		progressBar.setMaxProgress(maxProgress);
	}

	/**
	 * Set the progress message to be displayed in the progress bar
	 *
	 * @param progressMessage Progress message
	 */
	public void setProgressMessage(String progressMessage) {
		progressBar.setProgressMessage(progressMessage);
	}
}
