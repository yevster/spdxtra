package com.yevster.spdxtra.model.write;

import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.PropertyImpl;

import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;
import com.yevster.spdxtra.util.MiscUtils;

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
	 * Returns a disjunctive license containing or referecing all the provided
	 * licenses.
	 * 
	 * @param licenses
	 * @return
	 */
	public static License or(License... licenses) {
		return new CompoundLicense("DisjunctiveLicenseSet", licenses);
	}

	/**
	 * Returns a license with the specified text and the specified ID.
	 * 
	 * @param text License text
	 * @param name License name
	 * @param baseUrl The base URI of the containing document
	 * @param spdxId A unique SPDX ID of the license in the form "LicenseRef-*".
	 * @return
	 */
	public static License extracted(String text, String name, String baseUrl, String spdxId) {
		return extracted(text, name, baseUrl, spdxId, null);
	}
	
	/**
	 * Returns a license with the specified text and the specified ID.
	 * 
	 * @param text License text
	 * @param name License name
	 * @param baseUrl The base URI of the containing document
	 * @param spdxId A unique SPDX ID of the license in the form "LicenseRef-*".
	 * @param comment A comment to be stored inside the license. 
	 * @return
	 */
	public static License extracted(String text, String name, String baseUrl, String spdxId, String comment){
		return new ExtractedLicense(text, name, baseUrl, spdxId, MiscUtils.optionalOfBlankable(comment));
	}
	
	

	public static final License NOASSERTION = new SingleUriLicense(SpdxUris.NO_ASSERTION);
	public static final License NONE = new SingleUriLicense(SpdxUris.NONE);


	public abstract RDFNode getRdfNode(Model m);

}
