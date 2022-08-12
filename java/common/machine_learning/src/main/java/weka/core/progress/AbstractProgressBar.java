package weka.core.progress;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Handles common tasks between the GUI and Command line progress bar.
 */
public abstract class AbstractProgressBar implements Serializable {

	/**
	 * Is the progress bar set to indeterminate mode?.
	 */
	protected boolean m_indeterminate = false;

	/**
	 * Progress message, set by the user.
	 */
	protected String m_progressMessage;

	/**
	 * Actual progress value (e.g., 25)
	 */
	protected double m_actualProgress = 0;

	/**
	 * Maximum progress value/number of iterations (e.g., 50)
	 */
	protected double m_maxProgress = 0;

	/**
	 * Normalized progress value (e.g., 0.5)
	 */
	protected double m_normalizedProgress = 0;

	/**
	 * Estimated time remaining for the current task.
	 */
	protected String etaHms = "";

	/**
	 * Time the progress manager was started on (used for calculating ETA).
	 */
	protected long startTime;

	/**
	 * Create a new progress bar.
	 *
	 * @param maxProgress     Max progress the bar can reach.
	 * @param progressMessage Message to display.
	 */
	public AbstractProgressBar(double maxProgress, String progressMessage) {
		setMaxProgress(maxProgress);
		setProgressMessage(progressMessage);
	}

	/**
	 * Initialize all values to 0 and start the progress manager.
	 */
	public void start() {
		m_normalizedProgress = 0;
		m_actualProgress = 0;
		startTime = System.currentTimeMillis();
		calculate();
		onStart();
		refreshDisplay();
	}

	/**
	 * Do any required setup on start (e.g., show a popup window)
	 */
	protected abstract void onStart();

	/**
	 * Update the progress bar with the current value.
	 */
	protected abstract void onSetProgress();

	/**
	 * Helper methods so consumers don't need to keep track of iterations.
	 */
	public void increment() {
		setProgress(m_actualProgress + 1);
	}

	/**
	 * Perform any teardown (e.g., close popup window, display completion message)
	 */
	public abstract void finish();

	/**
	 * Update the progress bar display.
	 */
	public abstract void refreshDisplay();

	/**
	 * Calculates the normalized progress and ETA.
	 */
	private void calculate() {
		m_normalizedProgress = m_actualProgress / m_maxProgress;

		if (m_indeterminate) {
			return;
		}

		long eta = (long) ((m_maxProgress - m_actualProgress) * (System.currentTimeMillis() - startTime)
				/ m_actualProgress);

		etaHms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
				TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
				TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));
	}

	/**
	 * Main update method.
	 * Recalculates the current level of progress and refreshes the progress display.
	 *
	 * @param progress Actual progress value (e.g., current iteration number)
	 */
	public void setProgress(double progress) {
		// Limit the progress to the max previously set
		m_actualProgress = progress;
		if (!m_indeterminate)
			m_actualProgress = Math.min(m_actualProgress, m_maxProgress);

		calculate();
		onSetProgress();
		refreshDisplay();
	}

	/**
	 * Get the max progress value
	 *
	 * @return Maximum progress (e.g., max number of iterations)
	 */
	public double getMaxProgress() {
		return m_maxProgress;
	}

	/**
	 * Set the max progress value
	 *
	 * @param max Maximum value for progress (e.g., max number of iterations)
	 */
	public void setMaxProgress(double max) {
		m_maxProgress = max;
		m_indeterminate = max < 0;
	}

	/**
	 * Return the user supplied progress message
	 *
	 * @return User supplied progress message
	 */
	public String getProgressMessage() {
		return m_progressMessage;
	}

	/**
	 * Set the progress message
	 *
	 * @param message Progress Message (e.g., Processing data...)
	 */
	public void setProgressMessage(String message) {
		m_progressMessage = message;
	}

	/**
	 * Gets the estimated time remaining of the task, or "" if indeterminate
	 *
	 * @return Formatted ETA string
	 */
	public String getETAString() {
		return m_indeterminate ? "" : String.format("ETA: %s", etaHms);
	}

	/**
	 * Gets the indeterminate status of the progress bar
	 *
	 * @return Indeterminate status
	 */
	public boolean isIndeterminate() {
		return m_indeterminate;
	}

	/**
	 * Sets the indeterminate status of the progress bar
	 *
	 * @param m_indeterminate Indeterminate status
	 */
	public void setIndeterminate(boolean m_indeterminate) {
		this.m_indeterminate = m_indeterminate;
	}
}
