package com.yevster.spdxtra;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.tdb.TDBFactory;

import com.yevster.spdxtra.model.Creator;
import com.yevster.spdxtra.model.License;
import com.yevster.spdxtra.model.Relationship;
import com.yevster.spdxtra.model.SpdxDocument;
import com.yevster.spdxtra.model.Relationship.Type;
import com.yevster.spdxtra.model.SpdxElement;
import com.yevster.spdxtra.model.SpdxFile;
import com.yevster.spdxtra.model.SpdxPackage;

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
		public static ModelUpdate document(String baseUrl, String spdxId, String name, Creator creator,
				Creator... additionalCreators) {
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
				// Map the spdx prefix to the namespaces. Otherwise, when
				// written to RDF, the output will make children cry.
				model.getGraph().getPrefixMapping().setNsPrefix("spdx", SpdxUris.SPDX_TERMS);
				Resource type = model.createResource(SpdxUris.SPDX_DOCUMENT);
				Resource newResource = model.createResource(uri, type);
				newResource.addLiteral(SpdxProperties.SPDX_NAME, name);
				newResource.addProperty(SpdxProperties.DATA_LICENSE,
						model.createResource("http://spdx.org/licenses/CC0-1.0"));
				newResource.addProperty(SpdxProperties.SPEC_VERSION, Constants.DEFAULT_SPDX_VERSION);
				Resource creationInfo = model.createResource(SpdxResourceTypes.CREATION_INFO_TYPE);
				creationInfo.addProperty(SpdxProperties.CREATOR, creator.toString());
				creationInfo.addProperty(SpdxProperties.CREATION_DATE,
						ZonedDateTime.now(ZoneId.of("UTC")).format(Constants.SPDX_DATE_FORMATTER));
				for (Creator curCreator : additionalCreators) {
					creationInfo.addProperty(SpdxProperties.CREATOR, curCreator.toString());
				}

				newResource.addProperty(SpdxProperties.CREATION_INFO, creationInfo);
			};

		}
	}

	public static final class Document {
		/*
		 * For internal use inside a transaction only.
		 */
		private static final Resource getDocumentResource(Model model, String baseUrl, String documentSpdxId) {
			final String documentUri = baseUrl + "#" + documentSpdxId;
			return model.createResource(documentUri);

		}

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
				newPackage.addLiteral(SpdxProperties.PACKAGE_DOWNLOAD_LOCATION, NoneNoAssertionOrValue.AbsentValue.NOASSERTION.getUri());

			};
		}

		/**
		 * Updates the document's creation date.
		 * @param document
		 * @param dateTime
		 * @return
		 */
		public static ModelUpdate updateCreationDate(SpdxDocument document, ZonedDateTime dateTime) {
			return updateCreationDate(document.getDocumentNamespace(), document.getSpdxId(), dateTime);
		}

		/**
		 * Updates the document's creation date.
		 * 
		 * @param documentBaseUrl
		 * @param documentSpdxId
		 * @param dateTime
		 * @return
		 */
		public static ModelUpdate updateCreationDate(String documentBaseUrl, String documentSpdxId,
				ZonedDateTime dateTime) {
			ZonedDateTime utcTime = dateTime.withZoneSameInstant(ZoneId.of("UTC"));
			String newTimeToWrite = utcTime.format(Constants.SPDX_DATE_FORMATTER);
			return (model) -> {
				Resource document = getDocumentResource(model, documentBaseUrl, documentSpdxId);
				Resource creationInfo = document.getPropertyResourceValue(SpdxProperties.CREATION_INFO);
				Statement s = creationInfo.getProperty(SpdxProperties.CREATION_DATE);
				s.changeObject(newTimeToWrite);
			};

		}

	}

	public static final class Package {

		/**
		 * Generates an RDF update for the package name. Causes no change to any
		 * data inside pkg.
		 * 
		 * @param uri
		 * @param newName
		 * @return
		 */
		public static RdfResourceUpdate name(String uri, String newName) {
			return RdfResourceUpdate.updateStringProperty(uri, SpdxProperties.SPDX_NAME, newName);
		}

		/**
		 * Geneartes an RDF update for the package's copyright text.
		 * 
		 * @param uri
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

		/**
		 * Sets the filesAnalyzed property for the package.
		 * @param packageUri
		 * @param newValue
		 * @return
		 */
		public static RdfResourceUpdate filesAnalyzed(String packageUri, boolean newValue) {
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.FILES_ANALYZED,
					Boolean.toString(newValue));
		}

		/**
		 * Sets the package's file name attribute.
		 *
		 * @param packageUri
		 * @param fileName   Must not be null, empty, or only whitespace.
		 * @return
		 */
		public static RdfResourceUpdate packageFileName(String packageUri, String fileName) {
			if (StringUtils.isBlank(fileName)) {
				throw new IllegalArgumentException("File name must not be blank");
			}
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.PACKAGE_FILE_NAME, fileName);
		}

		/**
		 * Sets the package's download location.
		 *
		 * @param packageUri
		 * @param downloadLocation
		 * @return
		 */
		public static RdfResourceUpdate packageDownloadLocation(String packageUri, NoneNoAssertionOrValue downloadLocation) {
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.PACKAGE_DOWNLOAD_LOCATION, downloadLocation.getLiteralOrUriValue());
		}

	}

	public static final class File {
		/**
		 * Generates an RDF update for the file's concluded license
		 * 
		 * @param spdxFile
		 * @param license
		 * @return
		 */
		public static RdfResourceUpdate concludedLicense(SpdxFile spdxFile, final License license) {
			return concludedLicense(spdxFile.getUri(), license);
		}

		/**
		 * Generates an RDF update for the file's concluded license
		 * 
		 * @param fileUri
		 * @param license
		 * @return
		 */
		public static RdfResourceUpdate concludedLicense(String fileUri, final License license) {
			// Exactly the same property as in Package, so not duplicating.
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
