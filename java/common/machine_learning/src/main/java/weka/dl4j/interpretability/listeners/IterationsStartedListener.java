package weka.dl4j.interpretability.listeners;

/**
 * Event listener for when iterations increment.
 */
public interface IterationsStartedListener {

	/**
	 * Called when iterations start.
	 */
	void iterationsStarted(int maxIterations);
}
