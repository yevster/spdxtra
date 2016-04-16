package com.yevster.spdxtra.model.write;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.PropertyImpl;

import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;

public abstract class License {
	private static class SingleUriLicense extends License {
		final RDFNode uriNode;

		private SingleUriLicense(String uri) {
			uriNode = ResourceFactory.createResource(uri);
		}

		@Override
		public RDFNode getRdfNode(Model m) {
			return uriNode;
		}
	}

	private static class CompoundLicense extends License {

		private License[] memberLicenses;
		private String compoundElementName;

		public CompoundLicense(String compoundElementName, License... memberLicenses) {
			this.memberLicenses = memberLicenses;
			this.compoundElementName = compoundElementName;
		}

		public RDFNode getRdfNode(Model m) {
			Resource licenseType = ResourceFactory.createResource(SpdxUris.SPDX_TERMS + compoundElementName);
			Resource result = m.createResource(licenseType);
			for (License memberLicense : memberLicenses) {
				result.addProperty(SpdxProperties.LICENSE_MEMBER, memberLicense.getRdfNode(m));
			}
			return result;
		}

	}

	/**
	 * Returns a conjunctive license containing or referencing all the provided
	 * licenses.
	 *
	 * @param licenses
	 * @return
	 */
	public static License and(License... licenses) {
		return new CompoundLicense("ConjunctiveLicenseSet", licenses);
	}

	/**
	 * Returns a disjunctive license containing or referecing all the provided licenses.
	 * @param licenses
	 * @return
	 */
	public static License or(License... licenses) {
		return new CompoundLicense("DisjunctiveLicenseSet", licenses);
	}

	public static final License NOASSERTION = new SingleUriLicense(SpdxUris.NO_ASSERTION);
	public static final License NONE = new SingleUriLicense(SpdxUris.NONE);

	// Not all licenses have these property - conjunctive/disjunctive ones
	// don't.
	protected static final Property licenseNameProperty = new PropertyImpl(SpdxUris.SPDX_TERMS, "name");
	protected static final Property licenseIdProperty = new PropertyImpl(SpdxUris.SPDX_TERMS, "licenseId");

	public abstract RDFNode getRdfNode(Model m);

}
