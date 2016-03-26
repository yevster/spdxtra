package org.quackware.spdxtra.model;

import org.apache.jena.rdf.model.Resource;
import org.quackware.spdxtra.Namespaces;
import org.quackware.spdxtra.RdfResourceRepresentation;

/**
 * Describes an SPDX document.
 *
 */
public class SpdxDocument extends SpdxElement {
	public static final String RDF_TYPE = Namespaces.SPDX_TERMS + "SpdxDocument";

	
	public static class CreationInfo extends RdfResourceRepresentation {
		CreationInfo(Resource r) {
			super(r);
		}
	}

	public SpdxDocument(Resource resource) {
		super(resource);
	}

	public String getName() {
		return getPropertyAsString(Namespaces.SPDX_TERMS + "name");
	}

	public String getSpecVersion() {
		return getPropertyAsString(Namespaces.SPDX_TERMS + "specVersion");
	}

	public String getDocumentNamespace() {
		return getUri();
	}

}
