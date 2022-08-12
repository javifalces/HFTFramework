package weka.dl4j;

import weka.core.WekaException;

import java.nio.file.Paths;

public class ResourceResolver {

	public String GetResolvedPath(String locationInResources) throws WekaException {
		locationInResources = Paths.get("src", "main", "resources", locationInResources).toString();
		if (Utils.pathExists(locationInResources))
			return locationInResources;

		// Otherwise try the package home directory
		String packageHomeDir = Utils.defaultFileLocation();
		locationInResources = Paths.get(packageHomeDir, "wekaDeeplearning4j", locationInResources).toString();
		if (Utils.pathExists(locationInResources))
			return locationInResources;

		throw new WekaException(String.format("Cannot find specified resource: %s", locationInResources));
	}
}
