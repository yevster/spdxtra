package org.quackware.spdxtra;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.tdb.TDBFactory;

/**
 * @author yevster
 *
 * Static model manipulation logic
 *
 * Copyright (c) 2016 Yev Bronshteyn.
 * Committed under Apache-2.0 License
 */
public class ModelOperations {
	/**
	 * Reads inputFilePath and popualtes a new RDF data store at targetDirectoryPath with its contents.
	 * 
	 * @param inputFilePath Must be a valid path to an RDF file.
	 * @return
	 */
	public static DatasetInfo readFromFile(Path inputFilePath) {
		Objects.requireNonNull(inputFilePath);
		if (Files.notExists(inputFilePath) && Files.isRegularFile(inputFilePath)) throw new IllegalArgumentException("File "+inputFilePath.toAbsolutePath().toString()+" does not exist");
		final Path datasetPath;
		try{
			datasetPath = Files.createTempDirectory(inputFilePath.getFileName().toString());
		} catch (IOException ioe){
			throw new RuntimeException("Unable to create temp directory", ioe);
		}
		final InputStream is;
		try{
		is = Files.newInputStream(inputFilePath);
		} catch (IOException ioe){
			throw new RuntimeException("Unable to read file "+inputFilePath.toAbsolutePath().toString(), ioe);
		}
		Dataset dataset = TDBFactory.createDataset(datasetPath.toAbsolutePath().toString());
		try(DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.WRITE);){
			dataset.getDefaultModel().read(is, null);
			transaction.commit();
		}
		return new DatasetInfo(dataset, datasetPath);
		
	}
}
