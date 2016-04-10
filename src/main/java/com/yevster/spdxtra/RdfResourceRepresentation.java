package com.yevster.spdxtra;

import java.util.Objects;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;

public abstract class RdfResourceRepresentation {
	private final Resource rdfResource;

	protected RdfResourceRepresentation(Resource rdfResource) {
		this.rdfResource = rdfResource;
	}

	protected String getPropertyAsString(Property property) {
		Statement stmt = rdfResource.getProperty(property);
		if (stmt == null)
			return null;
		else
			return stmt.getString();
	}

	protected String getPropertyAsString(String propertyUri) {
		return getPropertyAsString(new PropertyImpl(propertyUri));

	}

	protected Resource getPropertyAsResource(String propertyUri) {
		return getPropertyAsResource(new PropertyImpl(propertyUri));
	}

	protected Resource getPropertyAsResource(Property property) {
		Statement stmt = rdfResource.getProperty(property);
		return stmt.getResource();
	}

	protected NoneNoAssertionOrValue getPropertyAsNoneNoAssertionOrValue(Property property) {
		Statement stmt = rdfResource.getProperty(property);

		if (stmt.getObject().isLiteral()) {
			return NoneNoAssertionOrValue.parse(stmt.getObject().asLiteral().getString());
		} else if ((SpdxUris.SPDX_TERMS + "noassertion").equals(stmt.getObject().asResource().getURI()))
			return NoneNoAssertionOrValue.NO_ASSERTION;
		else
			return NoneNoAssertionOrValue.NONE;
	}

	public String getUri() {
		return rdfResource.getURI();
	}

	@Override
	public boolean equals(Object obj) {
		// Must be the same subclass
		// Then, Use Resource's Equals implementation (compares URIs).
		// For SPDX purposes, this should be sufficient.
		return obj != null && Objects.equals(obj.getClass(), getClass())
				&& rdfResource.equals(((RdfResourceRepresentation) obj).rdfResource);

	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getClass().getName(), this.getUri());
	}

}
