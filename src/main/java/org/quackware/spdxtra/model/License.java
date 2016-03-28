package org.quackware.spdxtra.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.quackware.spdxtra.RdfResourceRepresentation;
import org.quackware.spdxtra.SpdxUris;

public abstract class License extends RdfResourceRepresentation {
	//Not all licenses have these property - conjunctive/disjunctive ones don't.
	protected static final Property licenseNameProperty = new PropertyImpl(SpdxUris.SPDX_TERMS, "name");
	protected static final Property licenseIdProperty = new PropertyImpl(SpdxUris.SPDX_TERMS, "licenseId");
	
	
	protected License(Resource r) {
		super(r);
	}
	


}
