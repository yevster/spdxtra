package com.yevster.spdxtra.model.write;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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

		final String label;

		private SingleUriLicense(String uri, String label) {
			uriNode = ResourceFactory.createResource(uri);
			this.label = label;
		}

		@Override
		public RDFNode getRdfNode(Model m) {
			return uriNode;
		}

		@Override
		public String getPrettyName() {
			return label;
		}
	}

	private static class CompoundLicense extends License {

		private final License[] memberLicenses;
		private final String compoundElementName;
		private final String operandLabel;

		public CompoundLicense(String compoundElementName, String operandLabel, License... memberLicenses) {
			this.memberLicenses = memberLicenses;
			this.compoundElementName = compoundElementName;
			this.operandLabel = operandLabel;
		}

		public RDFNode getRdfNode(Model m) {
			Resource licenseType = ResourceFactory.createResource(SpdxUris.SPDX_TERMS + compoundElementName);
			Resource result = m.createResource(licenseType);
			for (License memberLicense : memberLicenses) {
				result.addProperty(SpdxProperties.LICENSE_MEMBER, memberLicense.getRdfNode(m));
			}
			return result;
		}

		@Override
		public String getPrettyName() {
			return "(" + Arrays.stream(memberLicenses).map(License::getPrettyName)
					.collect(Collectors.joining(") " + operandLabel + " (")) + ")";

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
		return new CompoundLicense("ConjunctiveLicenseSet", "AND", licenses);
	}

	/**
	 * Returns a disjunctive license containing or referencing all the provided
	 * licenses.
	 * 
	 * @param licenses
	 * @return
	 */
	public static License or(License... licenses) {
		return new CompoundLicense("DisjunctiveLicenseSet", "OR", licenses);
	}

	/**
	 * Returns a license with the specified text and the specified ID.
	 * 
	 * @param text
	 *            License text
	 * @param name
	 *            License name
	 * @param baseUrl
	 *            The base URI of the containing document
	 * @param spdxId
	 *            A unique SPDX ID of the license in the form "LicenseRef-*".
	 * @return
	 */
	public static License extracted(String text, String name, String baseUrl, String spdxId) {
		return extracted(text, name, baseUrl, spdxId, null);
	}

	/**
	 * Returns a license with the specified text and the specified ID.
	 * 
	 * @param text
	 *            License text
	 * @param name
	 *            License name
	 * @param baseUrl
	 *            The base URI of the containing document
	 * @param spdxId
	 *            A unique SPDX ID of the license in the form "LicenseRef-*".
	 * @param comment
	 *            A comment to be stored inside the license.
	 * @return
	 */
	public static License extracted(String text, String name, String baseUrl, String spdxId, String comment) {
		return new ExtractedLicense(text, name, baseUrl, spdxId, MiscUtils.optionalOfBlankable(comment));
	}

	public static final License NOASSERTION = new SingleUriLicense(SpdxUris.NO_ASSERTION, "NOASSERTION");
	public static final License NONE = new SingleUriLicense(SpdxUris.NONE, "NONE");

	public abstract RDFNode getRdfNode(Model m);

	/**
	 * Returns a laconic but friendly name or a label for this license that may
	 * be displayed to a product user or written in a report.
	 */
	public abstract String getPrettyName();

}
