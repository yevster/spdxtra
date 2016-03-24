package org.quackware.spdxtra;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public abstract class RdfResourceRepresentation {
	private final Resource rdfResource;

	protected RdfResourceRepresentation(Resource rdfResource) {
		this.rdfResource = rdfResource;
	}

	public String getPropertyAsString(String propertyUri) {
		Statement stmt = rdfResource.getProperty(new PropertyImpl(propertyUri));
		if (stmt == null)
			return null;
		else
			return stmt.getString();
	}

	public Resource getPropertyAsResource(String propertyUri) {
		Statement stmt = rdfResource.getProperty(new PropertyImpl(propertyUri));
		return stmt.getResource();
	}

	public NoneNoAssertionOrValue getPropertyAsNoneNoAssertionOrValue(String propertyUri) {
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
