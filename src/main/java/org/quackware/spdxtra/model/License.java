package org.quackware.spdxtra.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.quackware.spdxtra.SpdxUris;

public abstract class License {
	private static class SingleUriLicense extends License {
		final RDFNode uriNode; 
		private SingleUriLicense(String uri) {
			uriNode = ResourceFactory.createResource(uri);
		}
		
		@Override
		public RDFNode getRdfNode() {
			return uriNode;
		}
	}
	


	public static final License NOASSERTION = new SingleUriLicense(SpdxUris.NO_ASSERTION);
	public static final License NONE = new SingleUriLicense(SpdxUris.NONE);

	// Not all licenses have these property - conjunctive/disjunctive ones
	// don't.
	protected static final Property licenseNameProperty = new PropertyImpl(SpdxUris.SPDX_TERMS, "name");
	protected static final Property licenseIdProperty = new PropertyImpl(SpdxUris.SPDX_TERMS, "licenseId");

	
	public abstract RDFNode getRdfNode();

}
