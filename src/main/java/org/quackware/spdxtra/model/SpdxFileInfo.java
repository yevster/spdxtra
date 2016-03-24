package org.quackware.spdxtra.model;

import org.apache.jena.ext.com.google.common.base.MoreObjects;
import org.apache.jena.rdf.model.Resource;
import org.quackware.spdxtra.Namespaces;
import org.quackware.spdxtra.RdfResourceRepresentation;

public class SpdxFileInfo extends RdfResourceRepresentation{
	public SpdxFileInfo(Resource resource) {
		super(resource);
	}

	
	public String getFileName(){
		return getPropertyAsString(Namespaces.SPDX_TERMS+"fileName");
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SpdxPackageInfo.class).add("File name", getFileName()).toString();
	}
}
