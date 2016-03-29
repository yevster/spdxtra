package org.quackware.spdxtra.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.quackware.spdxtra.SpdxUris;
import org.quackware.spdxtra.RdfResourceRepresentation;
import org.quackware.spdxtra.RdfResourceUpdate;

/**
 * Describes an SPDX document.
 *
 */
public class SpdxDocument extends SpdxElement implements SpdxIdentifiable {
	public static final String RDF_TYPE = SpdxUris.SPDX_DOCUMENT;
	
	private static final Property nameProperty = new PropertyImpl(SpdxUris.SPDX_TERMS + "name");

	
	public static class CreationInfo extends RdfResourceRepresentation {
		CreationInfo(Resource r) {
			super(r);
		}
	}

	public SpdxDocument(Resource resource) {
		super(resource);
	}

	public String getName() {
		return getPropertyAsString(nameProperty);
	}

	public String getSpecVersion() {
		return getPropertyAsString(SpdxUris.SPDX_TERMS + "specVersion");
	}

	public String getDocumentNamespace() {
		return getUri();
	}
	
	/* UPDATE GENERATORS */
	public static RdfResourceUpdate setName(SpdxDocument doc, String name){
		return RdfResourceUpdate.updateStringProperty(doc.getUri(), nameProperty, name);
	}
	
	/* Addition generators */

}
