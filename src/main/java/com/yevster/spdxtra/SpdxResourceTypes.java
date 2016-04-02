package com.yevster.spdxtra;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public final class SpdxResourceTypes {
	public static final Resource CREATION_INFO_TYPE = ResourceFactory.createResource(SpdxUris.SPDX_TERMS + "CreationInfo");

}
