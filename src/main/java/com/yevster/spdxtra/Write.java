package com.yevster.spdxtra;

import com.yevster.spdxtra.model.*;
import com.yevster.spdxtra.model.Creator.HumanCreator;
import com.yevster.spdxtra.model.write.License;
import com.yevster.spdxtra.util.MiscUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.base.Strings;
import org.apache.jena.ext.com.google.common.collect.Ordering;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.tdb.TDBFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Write {

	@FunctionalInterface
	public static interface ModelUpdate {
		void apply(Model model);
	}

	/**
	 * An model update that doesn't do anything.
	 */
	public static final ModelUpdate NOTHING = (m) -> {
	};

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
			Validate.baseUrl(baseUrl);
			Validate.spdxElementId(spdxId);
			Validate.notBlank(name);
			Validate.validate(!StringUtils.containsAny(name, '\n', '\r'), "Name must be a single line of text.");

			final String uri = baseUrl + "#" + spdxId;
			URI.create(uri); // An extra validity check

			return (model) -> {
				// Map the spdx prefix to the namespaces. Otherwise, when
				// written to RDF, the output will make children cry.
				model.getGraph().getPrefixMapping().setNsPrefix("spdx", SpdxUris.SPDX_TERMS);
				Resource newResource = model.createResource(uri, SpdxResourceTypes.DOCUMENT_TYPE);
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

		public static ModelUpdate annotation(String baseUrl, String parentSpdxId, Annotation.Type type, ZonedDateTime date,
											 Creator annotator, String comment) {
			// validation
			Validate.baseUrl(baseUrl);
			Validate.spdxElementId(parentSpdxId);
			Validate.notNull(type);
			Validate.notNull(date);

			final ZonedDateTime utcDate = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
			return (model) -> {

				Resource parentResource = model.getResource(baseUrl + "#" + parentSpdxId);
				if (parentResource.listProperties().toList().size() == 0) {
					// Parent resource does not exist.
					throw new IllegalArgumentException(
							"Parent resource " + parentResource.getURI() + " does not exist.");
				}

				Resource newAnnotationResource = model.createResource(SpdxResourceTypes.ANNOTATION_TYPE);
				newAnnotationResource.addLiteral(SpdxProperties.ANNOTATION_DATE,
						utcDate.format(Constants.SPDX_DATE_FORMATTER));
				newAnnotationResource.addProperty(SpdxProperties.ANNOTATION_TYPE, model.getResource(type.getUri()));

				newAnnotationResource.addLiteral(SpdxProperties.RDF_COMMENT, Strings.nullToEmpty(comment));

				newAnnotationResource.addProperty(SpdxProperties.ANNOTATOR, annotator.toString());
				parentResource.addProperty(SpdxProperties.ANNOTATION, newAnnotationResource);

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

		public static ModelUpdate addPackage(String documentBaseUrl, String documentSpdxId, String packageSpdxId,
				final String packageSpdxName) {

			Validate.spdxElementId(packageSpdxId);
			final String packageUri = documentBaseUrl + "#" + packageSpdxId;

			return (model) -> {
				Resource spdxPackageType = model.createResource(SpdxUris.SPDX_PACKAGE);
				Resource newPackage = model.createResource(packageUri, spdxPackageType);
				newPackage.addLiteral(SpdxProperties.SPDX_NAME, packageSpdxName);
				newPackage.addLiteral(SpdxProperties.PACKAGE_DOWNLOAD_LOCATION,
						NoneNoAssertionOrValue.AbsentValue.NOASSERTION.getUri());

			};
		}

		public static ModelUpdate addDescribedPackage(String documentBaseUrl, String documentSpdxId,
				String packageSpdxId, final String packageName) {
			Validate.baseUrl(documentBaseUrl);
			Validate.spdxElementId(documentSpdxId);
			Validate.spdxElementId(packageSpdxId);
			Validate.notBlank(packageName);

			ModelUpdate createPackage = addPackage(documentBaseUrl, documentSpdxId, packageSpdxId, packageName);

			final String packageUri = documentBaseUrl + "#" + packageSpdxId;
			final String documentUri = documentBaseUrl + "#" + documentSpdxId;

			return (model) -> {
				createPackage.apply(model);
				Write.addRelationship(documentUri, packageUri, Optional.empty(), Relationship.Type.DESCRIBES)
						.apply(model);
				Write.addRelationship(packageUri, documentUri, Optional.empty(), Relationship.Type.DESCRIBED_BY)
						.apply(model);

			};
		}

		/**
		 * Updates the document's creation date.
		 *
		 * @param document
		 * @param dateTime
		 * @return
		 */
		public static ModelUpdate creationDate(SpdxDocument document, ZonedDateTime dateTime) {
			return creationDate(document.getDocumentNamespace(), document.getSpdxId(), dateTime);
		}

		/**
		 * Updates the document's creation date.
		 *
		 * @param documentBaseUrl
		 * @param documentSpdxId
		 * @param dateTime
		 * @return
		 */
		public static ModelUpdate creationDate(String documentBaseUrl, String documentSpdxId, ZonedDateTime dateTime) {
			Validate.baseUrl(documentBaseUrl);
			Validate.spdxElementId(documentSpdxId);
			Validate.notNull(dateTime);
			ZonedDateTime utcTime = dateTime.withZoneSameInstant(ZoneId.of("UTC"));
			String newTimeToWrite = utcTime.format(Constants.SPDX_DATE_FORMATTER);
			return (model) -> {
				Resource document = getDocumentResource(model, documentBaseUrl, documentSpdxId);
				Resource creationInfo = document.getPropertyResourceValue(SpdxProperties.CREATION_INFO);
				Statement s = creationInfo.getProperty(SpdxProperties.CREATION_DATE);
				s.changeObject(newTimeToWrite);
			};
		}

		/**
		 * Generates an update that sets the creator comment.
		 * 
		 * @param documentBaseUrl
		 * @param documentSpdxId
		 * @param creationComment
		 * @return
		 */
		public static ModelUpdate creationComment(String documentBaseUrl, String documentSpdxId,
				String creationComment) {
			Validate.baseUrl(documentBaseUrl);
			Validate.spdxElementId(documentSpdxId);
			Validate.notNull(creationComment);
			return (model) -> {
				final String documentUri = documentBaseUrl + "#" + documentSpdxId;
				Resource resource = model.getResource(documentUri);
				if (!resource.listProperties().hasNext()) {
					throw new IllegalArgumentException("Document does not exist: " + documentUri);
				}
				Statement creationInfoStmt = resource.getProperty(SpdxProperties.CREATION_INFO);
				if (creationInfoStmt == null) {
					throw new IllegalArgumentException(
							"SPDX creation info not popertly initialized for document " + documentUri);
				}
				Resource creationInfo = creationInfoStmt.getObject().asResource();
				creationInfo.removeAll(SpdxProperties.RDF_COMMENT);
				creationInfo.addProperty(SpdxProperties.RDF_COMMENT, creationComment);
			};
		}

		/**
		 * Generates an update that sets the document comment.
		 */
		public static ModelUpdate comment(String documentBaseUrl, String documentSpdxId, String comment) {
			Validate.baseUrl(documentBaseUrl);
			Validate.spdxElementId(documentSpdxId);
			Validate.notNull(comment);
			return RdfResourceUpdate.updateStringProperty(documentBaseUrl + "#" + documentSpdxId,
					SpdxProperties.RDF_COMMENT, comment);
		}

		/**
		 * Sets the document specification version. NOTE: Some methods may not
		 * be supported for some versions. SpdXtra does not currently protect
		 * you from using elements that may be illegal in a non-default version.
		 * Alter this property at your own risk and peril. As of 2.0, required
		 * to be of the form "SPDX-M.N" where M and N are major and minor
		 * versions, respectively. See specification for details.
		 * 
		 * @param documentBaseUrl
		 * @param documentSpdxId
		 * @param specVersion
		 * @return
		 */
		public static ModelUpdate specVersion(String documentBaseUrl, String documentSpdxId, String specVersion) {
			return new RdfResourceUpdate(documentBaseUrl + "#" + documentSpdxId, SpdxProperties.SPEC_VERSION, false,
					(m) -> m.createLiteral(specVersion));
		}

	}

	public static final class Package {

		/**
		 * Generates an update for the package name. Causes no change to any
		 * data inside pkg.
		 *
		 * @param uri
		 * @param newName
		 * @return
		 */
		public static RdfResourceUpdate name(String uri, String newName) {
			Validate.spdxElementUri(uri);
			Validate.notBlank(newName);
			return RdfResourceUpdate.updateStringProperty(uri, SpdxProperties.SPDX_NAME, newName);
		}

		/**
		 * Generates an update that sets the package's version
		 */
		public static RdfResourceUpdate version(String uri, String newVersion) {
			Validate.spdxElementUri(uri);
			Validate.notBlank(newVersion);
			return RdfResourceUpdate.updateStringProperty(uri, SpdxProperties.PACKAGE_VERSION_INFO, newVersion);
		}

		/**
		 * Generates an update for the package's copyright text.
		 *
		 */
		public static RdfResourceUpdate copyrightText(String uri, NoneNoAssertionOrValue copyrightText) {
			Validate.spdxElementUri(uri);
			Validate.notNull(copyrightText);
			return RdfResourceUpdate.updateStringProperty(uri, SpdxProperties.COPYRIGHT_TEXT,
					copyrightText.getLiteralOrUriValue());
		}

		/**
		 * Generates an update for the packages checksum property, with one or
		 * more values.
		 */
		public static ModelUpdate checksums(String packageUri, String sha1, Checksum... others) {
			// Already implemented in files, with no differences in logic
			return File.checksums(packageUri, sha1, others);
		}

		/**
		 * Generates an update that adds a file with specified identifying
		 * information to the package. Note: the arguments of this method do not
		 * constitute the minimal necessary file information to produce a legal
		 * SPDX document. It is recommended that other properties of the file be
		 * set in the same transaction.
		 * 
		 * @param baseUrl
		 * @param pkgSpidxId
		 * @param fileSpdxId
		 * @param newFileName
		 * @return
		 */
		public static ModelUpdate addFile(String baseUrl, String pkgSpidxId, String fileSpdxId, String newFileName) {
			return Write.addNewFileToElement(baseUrl, pkgSpidxId, fileSpdxId, newFileName);
		}

		/**
		 * Generates an update for the package's declared license
		 *
		 * @param spdxPackage
		 * @param license
		 * @return
		 */
		public static ModelUpdate declaredLicense(SpdxPackage spdxPackage, final License license) {
			return declaredLicense(spdxPackage.getUri(), license);
		}

		/**
		 * Generates an update for the package's declared license
		 *
		 * @param packageUri
		 * @param license
		 * @return
		 */
		public static RdfResourceUpdate declaredLicense(String packageUri, final License license) {
			Validate.spdxElementUri(packageUri);
			Validate.notNull(license);
			return new RdfResourceUpdate(packageUri, SpdxProperties.LICENSE_DECLARED, false,
					(m) -> license.getRdfNode(m));
		}

		/**
		 * Generates an update for the package's concluded license
		 *
		 * @param spdxPackage
		 * @param license
		 * @return
		 */
		public static ModelUpdate concludedLicense(SpdxPackage spdxPackage, final License license) {
			return concludedLicense(spdxPackage.getUri(), license);
		}

		/**
		 * Generates an update for the package's concluded license
		 *
		 * @param packageUri
		 * @param license
		 * @return
		 */
		public static ModelUpdate concludedLicense(String packageUri, final License license) {
			Validate.notNull(license);
			Validate.spdxElementUri(packageUri);
			return new RdfResourceUpdate(packageUri, SpdxProperties.LICENSE_CONCLUDED, false,
					(m) -> license.getRdfNode(m));
		}

		/**
		 * Generates an update for the package's license comments.
		 */
		public static ModelUpdate licenseComments(String packageUri, String licenseComment) {
			Validate.spdxElementUri(packageUri);
			Validate.notNull(licenseComment);
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.LICENSE_COMMENTS, licenseComment);
		}

		/**
		 * Generates an update that sets the filesAnalyzed property for the
		 * package.
		 *
		 * @param packageUri
		 * @param newValue
		 * @return
		 */
		public static ModelUpdate filesAnalyzed(String packageUri, boolean newValue) {
			Validate.spdxElementUri(packageUri);
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.FILES_ANALYZED,
					Boolean.toString(newValue));
		}

		/**
		 * Generates an update that sets the package's file name attribute.
		 *
		 * @param packageUri
		 * @param fileName
		 *            Must not be null, empty, or only whitespace.
		 * @return
		 */
		public static ModelUpdate packageFileName(String packageUri, String fileName) {
			Validate.spdxElementUri(packageUri);
			Validate.notBlank(fileName);
			if (StringUtils.isBlank(fileName)) {
				throw new IllegalArgumentException("File name must not be blank");
			}
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.PACKAGE_FILE_NAME, fileName);
		}

		/**
		 * Generates an update that sets the package's download location.
		 *
		 * @param packageUri
		 * @param downloadLocation
		 * @return
		 */
		public static ModelUpdate packageDownloadLocation(String packageUri, NoneNoAssertionOrValue downloadLocation) {
			Validate.notNull(downloadLocation);
			Validate.spdxElementUri(packageUri);
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.PACKAGE_DOWNLOAD_LOCATION,
					downloadLocation.getLiteralOrUriValue());
		}

		/**
		 * Generates an update that sets the package's supplier.
		 */
		public static ModelUpdate supplier(String packageUri, HumanCreator supplier) {
			Validate.spdxElementUri(packageUri);
			Validate.notNull(supplier);
			String supplierString = supplier.toString();
			Validate.notBlank(supplierString);

			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.SUPPLIER, supplierString);
		}

		/**
		 * Generates an update that sets the package's originator.
		 */
		public static ModelUpdate originator(String packageUri, HumanCreator originator) {
			Validate.spdxElementUri(packageUri);
			Validate.notNull(originator);
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.ORIGINATOR, originator.toString());
		}

		/**
		 * Generates an update that adds another value of the
		 * "License Information from Files" property to any values that may have
		 * been previously specified.
		 */
		public static ModelUpdate addLicenseInfoFromFiles(String packageUri, License license) {
			Validate.spdxElementUri(packageUri);
			Validate.notNull(license);
			return new RdfResourceUpdate(packageUri, SpdxProperties.LICENSE_INFO_FROM_FILES, true, license::getRdfNode);
		}

		/**
		 * Generates an update that sets the package's homepage
		 */
		public static RdfResourceUpdate homepage(String packageUri, NoneNoAssertionOrValue homepage) {
			Validate.spdxElementUri(packageUri);
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.HOMEPAGE,
					homepage.getLiteralOrUriValue());
		}

		/**
		 * Generates an update that sets the package's summary description.
		 */
		public static ModelUpdate summary(String packageUri, String summary) {
			Validate.spdxElementUri(packageUri);
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.SUMMARY, summary);
		}

		/**
		 * Generates an update that sets the package's detailed description.
		 */
		public static ModelUpdate description(String packageUri, String description) {
			Validate.spdxElementUri(packageUri);
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.DESCRIPTION, description);
		}

		/**
		 * Generates an update that sets the value of the source information
		 * field, used to "provide additional information to describe any
		 * anomalies or discoveries in the determination of the origin of the
		 * package."
		 */
		public static ModelUpdate sourceInfo(String packageUri, String sourceInfo) {
			Validate.spdxElementUri(packageUri);
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.SOURCE_INFO, sourceInfo);
		}

		/**
		 * Generates an update that sets the package comment.
		 */
		public static ModelUpdate comment(String packageUri, String comment) {
			Validate.spdxElementUri(packageUri);
			return RdfResourceUpdate.updateStringProperty(packageUri, SpdxProperties.RDF_COMMENT, comment);
		}

		/**
		 * Computes the package's verification code - required for all packages
		 * where filesAnalyzed is true or omitted. Once executed, no new files
		 * should be added to the package and filesAnalyzed should not be
		 * modified. (not enforced)
		 */
		public static ModelUpdate finalize(String packageUri) {
			Validate.spdxElementUri(packageUri);
			return (Model m) -> {
				Resource packageResource = m.getResource(packageUri);
				// Package must exist.
				if (!packageResource.listProperties().hasNext()) {
					throw new IllegalArgumentException("Package " + packageUri + " does not exist.");
				}

				packageResource.removeAll(SpdxProperties.PACKAGE_VERIFICATION_CODE);

				// If filesAnalyzed=false, there must be no code.
				Statement filesAnalyzedStatement = packageResource.getProperty(SpdxProperties.FILES_ANALYZED);
				if (filesAnalyzedStatement != null && !filesAnalyzedStatement.getBoolean())
					return;

				StmtIterator it = packageResource.listProperties(SpdxProperties.HAS_FILE);
				Stream<Statement> statements = MiscUtils.toLinearStream(it);
				// We now have a bunch of triples whose objects are files.
				// Let's do some functional magic:

				// Get the objects of the triples as resources - SPDX Files
				String concatenatedSha1 = statements.map(Statement::getObject).map(RDFNode::asResource)
						// Get the checksums of these files (each file can have
						// multiple)
						.sorted(Write.byRequiredLiteralProperty(SpdxProperties.FILE_NAME))
						.flatMap(file -> MiscUtils.toLinearStream(file.listProperties(SpdxProperties.CHECKSUM)))
						// Convert them to resources
						.map(Statement::getObject).map(RDFNode::asResource)
						// Remove the non-Sha-1 checksums
						.filter(checksum -> StringUtils.equals(Checksum.Algorithm.SHA1.getUri(),
								checksum.getProperty(SpdxProperties.CHECKSUM_ALGORITHM).getObject().asResource()
										.getURI()))
						// Get the sha1 digests
						.map(checksum -> checksum.getProperty(SpdxProperties.CHECKSUM_VALUE).getObject().asLiteral()
								.getString())
						// Concatenate the SHA1 digests
						.collect(Collectors.joining());
				String verificationCode = DigestUtils.shaHex(concatenatedSha1);

				// Let's write it into the model.
				Resource pvcResource = m.createResource(SpdxResourceTypes.PACKAGE_VERIFICATION_CODE_TYPE);
				pvcResource.addLiteral(SpdxProperties.PACKAGE_VERIFICATION_CODE_VALUE, verificationCode);
				packageResource.addProperty(SpdxProperties.PACKAGE_VERIFICATION_CODE, pvcResource);

			};
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
		public static ModelUpdate concludedLicense(SpdxFile spdxFile, final License license) {
			return concludedLicense(spdxFile.getUri(), license);
		}

		/**
		 * Generates an update to set the file's concluded license
		 *
		 * @param fileUri
		 * @param license
		 * @return
		 */
		public static ModelUpdate concludedLicense(String fileUri, final License license) {
			// Exactly the same property as in Package, so not duplicating.
			return Write.Package.concludedLicense(fileUri, license);
		}

		/**
		 * Generates an update for the file's license comments.
		 */
		public static ModelUpdate licenseComments(String fileUri, String licenseComment) {
			return RdfResourceUpdate.updateStringProperty(fileUri, SpdxProperties.LICENSE_COMMENTS, licenseComment);
		}

		/**
		 * Generates an update to set the file's copyright text
		 * 
		 * @param fileUri
		 * @param copyrightText
		 * @return
		 */
		public static ModelUpdate copyrightText(String fileUri, NoneNoAssertionOrValue copyrightText) {
			// Exactly the same property as in Package, so not duplicating.
			return Write.Package.copyrightText(fileUri, copyrightText);
		}

		/**
		 * Generates an update to set the file comment
		 * 
		 * @param uri
		 * @param commentText
		 * @return
		 */
		public static ModelUpdate comment(String uri, String commentText) {
			Validate.notNull(commentText);
			Validate.spdxElementUri(uri);

			return new RdfResourceUpdate(uri, SpdxProperties.RDF_COMMENT, false,
					(m) -> ResourceFactory.createPlainLiteral(commentText));
		}

		/**
		 * Generates an update to set the file's artifactOf property (Deprecated
		 * in SPDX 2.1). Project name is required. If homepage is null or blank,
		 * UNKNOWN will be used.
		 */
		public static ModelUpdate artifactOf(String fileUri, String projectName, String projectHomepage) {
			Validate.notBlank(projectName);
			Validate.spdxElementUri(fileUri);

			return new RdfResourceUpdate(fileUri, SpdxProperties.ARTIFACT_OF, true, (m) -> {
				Resource doapProject = m.createResource(SpdxResourceTypes.DOAP_PROJECT);
				if (StringUtils.isNotBlank(projectHomepage)) {
					doapProject.addProperty(SpdxProperties.DOAP_HOMEPAGE, projectHomepage);
				}
				doapProject.addProperty(SpdxProperties.DOAP_NAME, projectName);
				return doapProject;
			});
		}

		/**
		 * Generates an update that adds a license found in the file to any
		 * existing values of this field.
		 */
		public static ModelUpdate addLicenseInfoInFile(String fileUri, final License license) {
			Validate.notNull(license);
			Validate.spdxElementUri(fileUri);
			return new RdfResourceUpdate(fileUri, SpdxProperties.LICENSE_INFO_IN_FILE, true,
					(Model m) -> license.getRdfNode(m));
		}

		/**
		 * Generates an RDF update for the file's type(s). Overwrites all
		 * previous values of this property on this file.
		 */
		public static ModelUpdate fileTypes(String fileUri, final FileType... fileTypes) {
			Validate.spdxElementUri(fileUri);
			Validate.noNulls((Object[])fileTypes);
			return (Model m) -> {
				Resource file = m.getResource(fileUri);
				file.removeAll(SpdxProperties.FILE_TYPE);
				for (FileType fileType : fileTypes) {
					file.addProperty(SpdxProperties.FILE_TYPE, ResourceFactory.createResource(fileType.getUri()));
				}
			};
		}

		/**
		 * Generates an update for the file's checksum(s). Overwrites all
		 * previous values of this proeprty.
		 * 
		 * @param fileUri
		 * @param sha1
		 *            - The SHA 1 digest. Required for all files.
		 * @param others
		 *            - Other, optional checksum values.
		 * @return
		 */
		public static ModelUpdate checksums(String fileUri, String sha1, Checksum... others) {
			Validate.spdxElementUri(fileUri);
			Validate.notNull(sha1);
			Validate.noNulls((Object[])others);

			return (Model m) -> {
				Resource file = m.getResource(fileUri);
				file.removeAll(SpdxProperties.CHECKSUM);
				file.addProperty(SpdxProperties.CHECKSUM, Checksum.sha1(sha1).asResource(m));
				for (Checksum checksum : others) {
					file.addProperty(SpdxProperties.CHECKSUM, checksum.asResource(m));
				}
			};
		}

		/**
		 * Generates an update that sets this file's notice text.
		 */
		public static ModelUpdate noticeText(String fileUri, String noticeText) {
			Validate.notNull(noticeText);
			Validate.spdxElementUri(fileUri);

			return RdfResourceUpdate.updateStringProperty(fileUri, SpdxProperties.NOTICE_TEXT, noticeText);
		}

		/**
		 * Generates an update that sets this files contributors. Any
		 * contributors previously set on this file will be replaced with the
		 * provided values when the update is applied.
		 * 
		 * @return
		 */
		public static ModelUpdate contributors(String fileUri, String... contributors) {
			Validate.spdxElementUri(fileUri);
			Validate.noNulls((Object[])contributors);
			return (m) -> {
				Resource file = m.getResource(fileUri);
				file.removeAll(SpdxProperties.FILE_CONTRIBUTOR);
				for (String contributor : contributors) {
					file.addProperty(SpdxProperties.FILE_CONTRIBUTOR, contributor);
				}
			};
		}

	}

	/**
	 * Convenience method for applying a small set of updates.
	 * 
	 * @param dataset
	 * @param updates
	 */
	public static void applyUpdatesInOneTransaction(Dataset dataset, ModelUpdate... updates) {
		applyUpdatesInOneTransaction(dataset, Arrays.asList(updates));
	}

	public static void applyUpdatesInOneTransaction(Dataset dataset, Iterable<? extends ModelUpdate> updates) {
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.WRITE)) {
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
		Validate.notNull(newDatasetPath);

		if (Files.notExists(newDatasetPath) || !Files.isDirectory(newDatasetPath)) {
			throw new IllegalArgumentException("Invalid dataset path: " + newDatasetPath.toAbsolutePath().toString());
		}
		Read.logger.debug("Creating new TDB in " + newDatasetPath.toAbsolutePath().toString());

		Dataset dataset = TDBFactory.createDataset(newDatasetPath.toString());
		dataset.getDefaultModel().getGraph().getPrefixMapping().setNsPrefix("spdx", SpdxUris.SPDX_TERMS);
		dataset.getDefaultModel().getGraph().getPrefixMapping().setNsPrefix("doap", SpdxUris.DOAP_NAMESPACE);
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
		Validate.notNull(inputFilePath);
		if (Files.notExists(inputFilePath) && Files.isRegularFile(inputFilePath))
			throw new IllegalArgumentException("File " + inputFilePath.toAbsolutePath().toString() + " does not exist");

		final InputStream is;
		try {
			is = Files.newInputStream(inputFilePath);
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to read file " + inputFilePath.toAbsolutePath().toString(), ioe);
		}

		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.WRITE)) {
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
		Validate.spdxElementUri(sourceUri);
		Validate.spdxElementUri(targetUri);
		Validate.noNulls(comment, type);

		return new RdfResourceUpdate(sourceUri, SpdxProperties.SPDX_RELATIONSHIP, true, (Model m) -> {

			Resource innerRelationship = m.createResource(new ResourceImpl(SpdxUris.SPDX_TERMS + "Relationship"));
			innerRelationship.addProperty(Relationship.relationshipTypeProperty, m.createResource(type.getUri()));
			if (comment.isPresent())
				innerRelationship.addProperty(SpdxProperties.RDF_COMMENT, m.createLiteral(comment.get()));
			innerRelationship.addProperty(Relationship.relatedElementProperty, m.getResource(targetUri));
			return innerRelationship;
		});
	}

	/**
	 * Adds a file to an SPDX element. Duplicate adds not currently handled.
	 * 
	 * @param baseUrl
	 * @param parentSpdxId
	 * @param newFileSpdxId
	 * @param newFileName
	 * @return
	 */
	private static ModelUpdate addNewFileToElement(String baseUrl, String parentSpdxId, String newFileSpdxId,
			String newFileName) {
		Validate.baseUrl(baseUrl);
		Validate.spdxElementId(parentSpdxId);
		Validate.spdxElementId(newFileSpdxId);
		final String parentUri = baseUrl + '#' + parentSpdxId;
		final String fileUri = baseUrl + '#' + newFileSpdxId;

		return (Model model) -> {
			Resource newFileResource = model.createResource(fileUri, SpdxResourceTypes.FILE_TYPE);
			/*
			 * There will always be one property (the type). If there are
			 * others, it means this file isn't new.
			 */
			if (newFileResource.listProperties().toSet().size() > 1) {
				// TODO: Fix, per issue #1.
				throw new UnsupportedOperationException(
						"File already exists. Adding existing files is currently unsupported.  " + newFileName);
			}
			newFileResource.addLiteral(SpdxProperties.FILE_NAME, newFileName);
			newFileResource.addProperty(SpdxProperties.COPYRIGHT_TEXT,
					NoneNoAssertionOrValue.NO_ASSERTION.getLiteralOrUriValue());
			Resource parentResource = model.createResource(parentUri);
			if (!parentResource.listProperties().hasNext()) { // Parent doesn't
																// exist.
				throw new IllegalArgumentException("Cannot add file to non-existing element " + parentUri);
			}
			parentResource.addProperty(SpdxProperties.HAS_FILE, newFileResource);
		};

	}

	/**
	 * Returns a comparator by required property. If the property is not
	 * present, a NullPointerException is thrown.
	 * 
	 * @param property
	 * @return
	 */
	protected static Comparator<Resource> byRequiredLiteralProperty(Property property) {
		return Ordering.from((Resource r1, Resource r2) -> {
			String s1 = r1.getProperty(property).getObject().asLiteral().getString();
			String s2 = r2.getProperty(property).getObject().asLiteral().getString();
			return Ordering.natural().nullsFirst().compare(s1, s2);
		}).nullsFirst();
	}

}
