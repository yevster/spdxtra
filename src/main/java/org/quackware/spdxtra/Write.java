package org.quackware.spdxtra;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.tdb.TDBFactory;
import org.quackware.spdxtra.model.SpdxPackage;

public final class Write {
	public static interface ModelUpdate {
		void apply(Model model);
	}

	/**
	 * Only useful when creating new resources, so this shouldn't be used
	 * anywhere else.
	 */
	private static final Property rdfTypeProperty = new PropertyImpl(SpdxUris.RDF_NAMESPACE, "type");

	public static final class New {
		/**
		 * Creates a dataset update that creates a document.
		 * 
		 * @param baseUrl
		 * @param spdxId
		 * @return
		 */
		public static ModelUpdate document(String baseUrl, String spdxId, String name) {
			// validation
			if (StringUtils.isBlank(baseUrl) || StringUtils.containsAny(baseUrl, '#')) {
				throw new IllegalArgumentException("Illegal base URL: " + baseUrl);
			}
			if (StringUtils.isBlank(spdxId) || StringUtils.containsAny(spdxId, '#', ':') || !StringUtils.startsWith(spdxId, "SPDXRef-")) {
				throw new IllegalArgumentException("Illegal spdxId. Must be of the form SPDXRef-*");
			}
			if (StringUtils.isBlank(name) || StringUtils.containsAny(name, '\n','\r')) {
				throw new IllegalArgumentException("Name cannot be null or blank and must be a single line of text.");
			}
			final String uri = baseUrl + "#" + spdxId;
			URI.create(uri); // An extra validity check.
			return (model) -> {
				Resource type = model.createResource(SpdxUris.SPDX_DOCUMENT);
				Resource newResource = model.createResource(uri, type);
				newResource.addLiteral(SpdxProperties.SPDX_NAME, name);
			};

		}
	}

	public static final class Document {

	}

	public static final class Package {

		/**
		 * Generates an RDF update for the package name. Causes no change to any
		 * data inside pkg.
		 * 
		 * @param pkg
		 * @param newName
		 * @return
		 */
		public static RdfResourceUpdate name(SpdxPackage pkg, String newName) {
			return RdfResourceUpdate.updateStringProperty(pkg.getUri(), SpdxProperties.PACKAGE_NAME, newName);
		}

	}

	public static void applyUpdatesInOneTransaction(Dataset dataset, Iterable<? extends ModelUpdate> updates) {
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.WRITE);) {
			Model model = dataset.getDefaultModel();
			for (ModelUpdate update : updates) {
				update.apply(model);

			}
			transaction.commit();
		}

	}

	/**
	 * Reads inputFilePath and populates a new RDF data store at
	 * targetDirectoryPath with its contents.
	 * 
	 * @param inputFilePath
	 *            Must be a valid path to an RDF file.
	 * @param newDatasetPath
	 *            The path to which to persist the RDF triple store with SPDX
	 *            data.
	 * @return
	 */
	public static Dataset rdfIntoNewDataset(Path inputFilePath, Path newDatasetPath) {
		Objects.requireNonNull(newDatasetPath);
	
		if (Files.notExists(newDatasetPath) || !Files.isDirectory(newDatasetPath)) {
			throw new IllegalArgumentException("Invalid dataset path: " + newDatasetPath.toAbsolutePath().toString());
		}
		Read.logger.debug("Creating new TDB in " + newDatasetPath.toAbsolutePath().toString());
	
		Dataset dataset = TDBFactory.createDataset(newDatasetPath.toString());
		dataset.getDefaultModel().getGraph().getPrefixMapping().setNsPrefix("spdx", SpdxUris.SPDX_TERMS);
		Write.rdfIntoDataset(inputFilePath, dataset);
		return dataset;
	}

	/**
	 * Reads RDF from inputFilePath into a provided dataset. NOTE: This behavior
	 * is not tested with pre-populated datasets.
	 * 
	 * @param inputFilePath
	 * @param dataset
	 */
	public static void rdfIntoDataset(Path inputFilePath, Dataset dataset) {
		Objects.requireNonNull(inputFilePath);
		if (Files.notExists(inputFilePath) && Files.isRegularFile(inputFilePath))
			throw new IllegalArgumentException("File " + inputFilePath.toAbsolutePath().toString() + " does not exist");
	
		final InputStream is;
		try {
			is = Files.newInputStream(inputFilePath);
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to read file " + inputFilePath.toAbsolutePath().toString(), ioe);
		}
	
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.WRITE);) {
			dataset.getDefaultModel().read(is, null);
			transaction.commit();
		}
	
	}

}
