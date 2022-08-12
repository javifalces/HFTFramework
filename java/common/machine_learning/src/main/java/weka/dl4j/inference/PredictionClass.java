package weka.dl4j.inference;

/**
 * Wrapper class to hold a class ID and classname.
 */
public class PredictionClass {

	/**
	 * ID of the class prediction.
	 */
	private final int classID;

	/**
	 * Human readable class name.
	 */
	private final String className;

	/**
	 * Create a new PredictionClass.
	 *
	 * @param classID   Class ID
	 * @param className Class Name
	 */
	public PredictionClass(int classID, String className) {
		this.classID = classID;
		this.className = className;
	}

	public int getClassID() {
		return classID;
	}

	public String getClassName() {
		return className;
	}

	@Override public String toString() {
		return String.format("Class ID = %d, Class name = %s", getClassID(), getClassName());
	}
}
