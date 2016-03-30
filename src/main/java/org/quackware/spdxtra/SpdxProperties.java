package org.quackware.spdxtra;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public final class SpdxProperties {


	public static final Property PACKAGE_VERSION_INFO = new PropertyImpl(SpdxUris.SPDX_TERMS + "versionInfo");
	public static final Property COPYRIGHT_TEXT=new PropertyImpl(SpdxUris.SPDX_TERMS + "copyrightText");
	public static final Property SPDX_NAME = new PropertyImpl(SpdxUris.SPDX_TERMS + "name");
	public static final Property SPDX_RELATIONSHIP = new PropertyImpl(SpdxUris.SPDX_RELATIONSHIP);
	public static final Property RDF_COMMENT = new PropertyImpl(SpdxUris.RDFS_NAMESPACE, "comment");
	public static final Property RDF_TYPE = new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
}
