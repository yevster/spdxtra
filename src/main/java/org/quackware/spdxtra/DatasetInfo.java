package org.quackware.spdxtra;

import java.nio.file.Path;

import org.apache.jena.query.Dataset;

public class DatasetInfo {
	private Dataset dataset;
	private Path datasetPath;
	
	
	public DatasetInfo(Dataset dataset, Path datasetPath) {
		this.dataset = dataset;
		this.datasetPath = datasetPath;
	}
	
	public Dataset getDataset() {
		return dataset;
	}
	public Path getDatasetPath() {
		return datasetPath;
	}
	
	
}
