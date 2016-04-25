package com.yevster.spdxtra.model;

import com.google.common.base.MoreObjects;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;

public class SpdxFile extends SpdxElement implements SpdxIdentifiable {
	public static final String RDF_TYPE = SpdxUris.SPDX_TERMS + "File";

	public SpdxFile(Resource resource) {
		super(resource);
		String resourceType = resource.getProperty(SpdxProperties.RDF_TYPE).getObject().asResource().getURI();
		if (!StringUtils.equals(resourceType, SpdxUris.SPDX_FILE)) {
			throw new IllegalArgumentException("Resource " + resource.getURI() + " is not an SPDX file");
		}	
	}

	public String getFileName() {
		return getPropertyAsString(SpdxUris.SPDX_TERMS + "fileName");
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SpdxPackage.class).add("File name", getFileName()).toString();
	}
}
