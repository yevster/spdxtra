package com.yevster.spdxtra;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public final class SpdxProperties {

	public static final Property COPYRIGHT_TEXT = new PropertyImpl(SpdxUris.SPDX_TERMS + "copyrightText");
	public static final Property SPDX_NAME = new PropertyImpl(SpdxUris.SPDX_TERMS + "name");
	public static final Property SPDX_RELATIONSHIP = new PropertyImpl(SpdxUris.SPDX_RELATIONSHIP);
	public static final Property RDF_COMMENT = new PropertyImpl(SpdxUris.RDFS_NAMESPACE, "comment");
	public static final Property RDF_TYPE = new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	public static final Property RDF_ID = new PropertyImpl(SpdxUris.RDF_NAMESPACE, "ID");

	/// DOCUMENT PROPERTIES
	public static final Property DATA_LICENSE = new PropertyImpl(SpdxUris.SPDX_TERMS, "dataLicense");
	public static final Property CREATION_INFO = new PropertyImpl(SpdxUris.SPDX_TERMS, "creationInfo");
	public static final Property CREATOR = new PropertyImpl(SpdxUris.SPDX_TERMS, "creator");
	public static final Property CREATION_DATE = new PropertyImpl(SpdxUris.SPDX_TERMS, "created");
	public static final Property SPEC_VERSION = new PropertyImpl(SpdxUris.SPDX_TERMS, "specVersion");

	// PACKAGE PROPERTIES
	public static final Property PACKAGE_VERSION_INFO = new PropertyImpl(SpdxUris.SPDX_TERMS + "versionInfo");
	public static final Property PACKAGE_DOWNLOAD_LOCATION = new PropertyImpl(SpdxUris.SPDX_TERMS, "downloadLocation");
	public static final Property PACKAGE_FILE_NAME = new PropertyImpl(SpdxUris.SPDX_TERMS, "packageFileName");
	public static final Property PACKAGE_VERIFICATION_CODE = new PropertyImpl(SpdxUris.SPDX_TERMS, "packageVerificationCode");
	public static final Property PACKAGE_VERIFICATION_CODE_VALUE = new PropertyImpl(SpdxUris.SPDX_TERMS, "packageVerificationCodeValue");
	public static final Property FILES_ANALYZED = new PropertyImpl(SpdxUris.SPDX_TERMS, "filesAnalyzed");
	public static final Property HOMEPAGE = new PropertyImpl(SpdxUris.DOAP_NAMESPACE, "homepage");

	// FILE PROPERTIES
	public static final Property HAS_FILE = new PropertyImpl(SpdxUris.SPDX_TERMS, "hasFile");
	public static final Property FILE_NAME = new PropertyImpl(SpdxUris.SPDX_TERMS, "fileName");
	public static final Property FILE_TYPE = new PropertyImpl(SpdxUris.SPDX_TERMS, "fileType");
	public static final Property CHECKSUM = new PropertyImpl(SpdxUris.SPDX_TERMS, "checksum");
	public static final Property CHECKSUM_VALUE = new PropertyImpl(SpdxUris.SPDX_TERMS, "checksumValue");
	public static final Property CHECKSUM_ALGORITHM = new PropertyImpl(SpdxUris.SPDX_TERMS, "algorithm");

	// LICENSES
	public static final Property LICENSE_DECLARED = new PropertyImpl(SpdxUris.SPDX_TERMS, "licenseDeclared");
	public static final Property LICENSE_CONCLUDED = new PropertyImpl(SpdxUris.SPDX_TERMS, "licenseConcluded");
	public static final Property LICENSE_MEMBER = new PropertyImpl(SpdxUris.SPDX_TERMS, "member");
	public static final Property LICENSE_INFO_IN_FILE = new PropertyImpl(SpdxUris.SPDX_TERMS, "licenseInfoInFile");
	public static final Property LICENSE_EXTRACTED_TEXT = new PropertyImpl(SpdxUris.SPDX_TERMS, "extractedText");
	public static final Property LICENSE_ID = new PropertyImpl(SpdxUris.SPDX_TERMS, "licenseId");
	
	// ANNOTATIONS
	public static final Property ANNOTATION = new PropertyImpl(SpdxUris.SPDX_TERMS, "annotation");
	public static final Property ANNOTATION_TYPE= new PropertyImpl(SpdxUris.SPDX_TERMS, "annotationType");
	public static final Property ANNOTATION_DATE = new PropertyImpl(SpdxUris.SPDX_TERMS, "annotationDate");
	public static final Property ANNOTATOR = new PropertyImpl(SpdxUris.SPDX_TERMS, "annotator");
}
