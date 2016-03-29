package org.quackware.spdxtra;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public final class SpdxProperties {

	public static final Property PACKAGE_NAME = new PropertyImpl(SpdxUris.SPDX_TERMS + "name");
	public static final Property PACKAGE_VERSION_INFO = new PropertyImpl(SpdxUris.SPDX_TERMS + "versionInfo");
	public static final Property COPYRIGHT_TEXT=new PropertyImpl(SpdxUris.SPDX_TERMS + "copyrightText");
}
