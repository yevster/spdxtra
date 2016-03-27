package org.quackware.spdxtra.model;

import org.apache.jena.rdf.model.Resource;
import org.quackware.spdxtra.SpdxUris;
import org.quackware.spdxtra.RdfResourceRepresentation;

/**
 * Describes an SPDX document.
 *
 */
public class SpdxDocument extends SpdxElement {
	public static final String RDF_TYPE = SpdxUris.SPDX_TERMS + "SpdxDocument";

	
	public static class CreationInfo extends RdfResourceRepresentation {
		CreationInfo(Resource r) {
			super(r);
		}
	}

	public SpdxDocument(Resource resource) {
		super(resource);
	}

	public String getName() {
		return getPropertyAsString(SpdxUris.SPDX_TERMS + "name");
	}

	public String getSpecVersion() {
		return getPropertyAsString(SpdxUris.SPDX_TERMS + "specVersion");
	}

	public String getDocumentNamespace() {
		return getUri();
	}

}
