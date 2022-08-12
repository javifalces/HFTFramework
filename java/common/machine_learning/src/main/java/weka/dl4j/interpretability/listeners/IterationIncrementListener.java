package weka.dl4j.interpretability.listeners;

/**
 * Event listener for when iterations increment.
 */
public interface IterationIncrementListener {

	/**
	 * Called when iterations incremented.
	 */
	void iterationIncremented();
}
