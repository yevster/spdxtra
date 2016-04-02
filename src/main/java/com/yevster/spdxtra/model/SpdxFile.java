package com.yevster.spdxtra.model;

import com.google.common.base.MoreObjects;
import com.yevster.spdxtra.SpdxUris;

import org.apache.jena.rdf.model.Resource;

public class SpdxFile extends SpdxElement implements SpdxIdentifiable {
	public static final String RDF_TYPE = SpdxUris.SPDX_TERMS + "File";

	public SpdxFile(Resource resource) {
		super(resource);
	}

	public String getFileName() {
		return getPropertyAsString(SpdxUris.SPDX_TERMS + "fileName");
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SpdxPackage.class).add("File name", getFileName()).toString();
	}
}
