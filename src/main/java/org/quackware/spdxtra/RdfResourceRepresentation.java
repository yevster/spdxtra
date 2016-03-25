package org.quackware.spdxtra;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public abstract class RdfResourceRepresentation {
	private final Resource rdfResource;
	public static final Property RDF_TYPE = new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

	protected RdfResourceRepresentation(Resource rdfResource) {
		this.rdfResource = rdfResource;
	}

	protected String getPropertyAsString(String propertyUri) {
		Statement stmt = rdfResource.getProperty(new PropertyImpl(propertyUri));
		if (stmt == null)
			return null;
		else
			return stmt.getString();
	}

	protected Resource getPropertyAsResource(String propertyUri) {
		Statement stmt = rdfResource.getProperty(new PropertyImpl(propertyUri));
		return stmt.getResource();
	}

	protected NoneNoAssertionOrValue getPropertyAsNoneNoAssertionOrValue(String propertyUri) {
		Resource r = getPropertyAsResource(propertyUri);
		if (r.isLiteral()) {
			return NoneNoAssertionOrValue.of(r.asLiteral().getString());
		} else if ((Namespaces.SPDX_TERMS + "noassertion").equals(r.asResource().getURI()))
			return NoneNoAssertionOrValue.NO_ASSERTION;
		else
			return NoneNoAssertionOrValue.NONE;
	}

	public String getUri() {
		return rdfResource.getURI();
	}

}
