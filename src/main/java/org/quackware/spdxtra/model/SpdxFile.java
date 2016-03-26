package org.quackware.spdxtra.model;

import org.apache.jena.ext.com.google.common.base.MoreObjects;
import org.apache.jena.rdf.model.Resource;
import org.quackware.spdxtra.Namespaces;

public class SpdxFile extends SpdxElement implements SpdxIdentifiable {
	public static final String RDF_TYPE = Namespaces.SPDX_TERMS + "File";

	public SpdxFile(Resource resource) {
		super(resource);
	}

	public String getFileName() {
		return getPropertyAsString(Namespaces.SPDX_TERMS + "fileName");
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SpdxPackage.class).add("File name", getFileName()).toString();
	}
}
