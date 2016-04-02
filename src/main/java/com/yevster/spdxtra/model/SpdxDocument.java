package com.yevster.spdxtra.model;

import org.apache.jena.rdf.model.Resource;

import com.yevster.spdxtra.RdfResourceRepresentation;
import com.yevster.spdxtra.RdfResourceUpdate;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;

/**
 * Describes an SPDX document.
 *
 */
public class SpdxDocument extends SpdxElement implements SpdxIdentifiable {
	public static final String RDF_TYPE = SpdxUris.SPDX_DOCUMENT;
	
	public static class CreationInfo extends RdfResourceRepresentation {
		CreationInfo(Resource r) {
			super(r);
		}
	}

	public SpdxDocument(Resource resource) {
		super(resource);
	}

	public String getName() {
		return getPropertyAsString(SpdxProperties.SPDX_NAME);
	}

	public String getSpecVersion() {
		return getPropertyAsString(SpdxUris.SPDX_TERMS + "specVersion");
	}

	public String getDocumentNamespace() {
		return getUri();
	}
	
	/* UPDATE GENERATORS */
	public static RdfResourceUpdate setName(SpdxDocument doc, String name){
		return RdfResourceUpdate.updateStringProperty(doc.getUri(), SpdxProperties.SPDX_NAME, name);
	}
	
	/* Addition generators */

}
