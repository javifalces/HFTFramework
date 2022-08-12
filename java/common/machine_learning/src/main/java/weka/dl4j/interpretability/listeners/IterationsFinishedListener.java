package weka.dl4j.interpretability.listeners;

/**
 * Event listener for when iterations finish.
 */
public interface IterationsFinishedListener {

	/**
	 * Called when iterations finish.
	 */
	void iterationsFinished();
}
