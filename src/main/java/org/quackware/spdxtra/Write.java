package org.quackware.spdxtra;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.tdb.TDBFactory;
import org.quackware.spdxtra.model.License;
import org.quackware.spdxtra.model.Relationship;
import org.quackware.spdxtra.model.Relationship.Type;
import org.quackware.spdxtra.model.SpdxElement;
import org.quackware.spdxtra.model.SpdxFile;
import org.quackware.spdxtra.model.SpdxPackage;

public final class Write {
	@FunctionalInterface
	public static interface ModelUpdate {
		void apply(Model model);
	}


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
			if (!Validate.spdxId(spdxId)) {
				throw new IllegalArgumentException("Illegal spdxId. Must be of the form SPDXRef-*");
			}
			if (StringUtils.isBlank(name) || StringUtils.containsAny(name, '\n', '\r')) {
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
		public static ModelUpdate addDescribedPackage(String documentBaseUrl, String documentSpdxId,
				String packageSpdxId, final String packageSpdxName) {
			if (!Validate.spdxId(packageSpdxId)) {
				throw new IllegalArgumentException("SPDX ID must be in the form SPDXRef-*");
			}
			final String packageUri = documentBaseUrl + "#" + packageSpdxId;
			final String documentUri = documentBaseUrl + "#" + documentSpdxId;

			return (model) -> {
				Resource spdxPackageType = model.createResource(SpdxUris.SPDX_PACKAGE);
				Resource newPackage = model.createResource(packageUri, spdxPackageType);
				newPackage.addLiteral(SpdxProperties.SPDX_NAME, packageSpdxName);
				Write.addRelationship(documentUri, packageUri, Optional.empty(), Type.DESCRIBES).apply(model);
				Write.addRelationship(packageUri, documentUri, Optional.empty(), Type.DESCRIBED_BY).apply(model);

			};
		}

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
		public static RdfResourceUpdate name(String uri, String newName) {
			return RdfResourceUpdate.updateStringProperty(uri, SpdxProperties.SPDX_NAME, newName);
		}

		/**
		 * Geneartes an RDF update for the package's copyright text.
		 * 
		 * @param pkg
		 * @param copyrightText
		 * @return
		 */
		public static RdfResourceUpdate copyrightText(String uri, NoneNoAssertionOrValue copyrightText) {
			return RdfResourceUpdate.updateStringProperty(uri, SpdxProperties.COPYRIGHT_TEXT,
					copyrightText.getLiteralOrUriValue());
		}

		/**
		 * Generates an RDF update for the package's declared license
		 * 
		 * @param spdxPackage
		 * @param license
		 * @return
		 */
		public static RdfResourceUpdate declaredLicense(SpdxPackage spdxPackage, final License license) {
			return declaredLicense(spdxPackage.getUri(), license);
		}

		/**
		 * Generates an RDF update for the package's declared license
		 * 
		 * @param packageUri
		 * @param license
		 * @return
		 */
		public static RdfResourceUpdate declaredLicense(String packageUri, final License license) {
			return new RdfResourceUpdate(packageUri, SpdxProperties.LICENSE_DECLARED, false,
					(m) -> license.getRdfNode());
		}
		
		/**
		 * Generates an RDF update for the package's concluded license
		 * 
		 * @param spdxPackage
		 * @param license
		 * @return
		 */
		public static RdfResourceUpdate concludedLicense(SpdxPackage spdxPackage, final License license) {
			return concludedLicense(spdxPackage.getUri(), license);
		}
		
		/**
		 * Generates an RDF update for the package's concluded license
		 * 
		 * @param packageUri
		 * @param license
		 * @return
		 */
		public static RdfResourceUpdate concludedLicense(String packageUri, final License license) {
			return new RdfResourceUpdate(packageUri, SpdxProperties.LICENSE_CONCLUDED, false,
					(m) -> license.getRdfNode());
		}

	}
	
	public static final class File{
		/**
		 * Generates an RDF update for the file's concluded license
		 * 
		 * @param spdxPackage
		 * @param license
		 * @return
		 */
		public static RdfResourceUpdate concludedLicense(SpdxFile spdxFile, final License license) {
			return concludedLicense(spdxFile.getUri(), license);
		}
		
		/**
		 * Generates an RDF update for the file's concluded license
		 * 
		 * @param packageUri
		 * @param license
		 * @return
		 */
		public static RdfResourceUpdate concludedLicense(String fileUri, final License license){
			//Exactly the same property as in Package, so not duplicating.
			return Write.Package.concludedLicense(fileUri, license);
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

	public static RdfResourceUpdate addRelationship(SpdxElement source, SpdxElement target,
			final Optional<String> comment, final Relationship.Type type) {
		return addRelationship(source.getUri(), target.getUri(), comment, type);
	}

	public static RdfResourceUpdate addRelationship(String sourceUri, String targetUri, final Optional<String> comment,
			final Relationship.Type type) {

		return new RdfResourceUpdate(sourceUri, SpdxProperties.SPDX_RELATIONSHIP, true, (Model m) -> {

			Resource innerRelationship = m.createResource(new ResourceImpl(SpdxUris.SPDX_TERMS + "Relationship"));
			innerRelationship.addProperty(Relationship.relationshipTypeProperty, m.createResource(type.getUri()));
			if (comment.isPresent())
				innerRelationship.addProperty(SpdxProperties.RDF_COMMENT, m.createLiteral(comment.get()));
			innerRelationship.addProperty(Relationship.relatedElementProperty, m.getResource(targetUri));
			return innerRelationship;
		});
	}

}
