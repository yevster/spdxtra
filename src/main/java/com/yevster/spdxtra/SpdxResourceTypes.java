package com.yevster.spdxtra;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public final class SpdxResourceTypes {
	public static final Resource CREATION_INFO_TYPE = ResourceFactory.createResource(SpdxUris.SPDX_TERMS + "CreationInfo");
	public static final Resource DOCUMENT_TYPE = ResourceFactory.createResource(SpdxUris.SPDX_DOCUMENT);
	public static final Resource FILE_TYPE = ResourceFactory.createResource(SpdxUris.SPDX_FILE);
	public static final Resource CHECKSUM_TYPE = ResourceFactory.createResource(SpdxUris.SPDX_TERMS + "Checksum");
	public static final Resource PACKAGE_VERIFICATION_CODE_TYPE = ResourceFactory
			.createResource(SpdxUris.SPDX_TERMS + "PackageVerificationCode");
}
