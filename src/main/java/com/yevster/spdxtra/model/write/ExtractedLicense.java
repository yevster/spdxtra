package com.yevster.spdxtra.model.write;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;
import com.yevster.spdxtra.Validate;

/**
 * Represents an SPDX extracted license (i.e. a license whose full text is
 * obtained from a file)
 * 
 * @author ybronshteyn
 *
 */
class ExtractedLicense extends License {
	private static final Resource extractedLicenseType = ResourceFactory.createResource(SpdxUris.SPDX_TERMS + "ExtractedLicensingInfo");

	private String text;

	private String spdxId;

	private String baseUrl;

	public ExtractedLicense(String text, String baseUrl, String spdxId) {
		if (!Validate.spdxLicenseId(spdxId))
			throw new IllegalArgumentException("Illegal SPDX ID " + spdxId);
		if (!Validate.baseUrl(baseUrl)) {
			throw new IllegalArgumentException("Illegal base URL: " + baseUrl);
		}
		if (StringUtils.isBlank(text))
			throw new IllegalArgumentException("License text cannot be null or blank");
		this.spdxId = spdxId;
		this.text = text;
		this.baseUrl = baseUrl;
	}

	@Override
	public RDFNode getRdfNode(Model m) {
		String licenseUri = this.baseUrl + '#' + this.spdxId;

		Resource resource = m.createResource(licenseUri, extractedLicenseType);
		if (resource.listProperties().hasNext()) {
			// Existing resource. Need to remove properties before writing them.
			resource.removeAll(SpdxProperties.LICENSE_EXTRACTED_TEXT);
			resource.removeAll(SpdxProperties.LICENSE_ID);
		}
		resource.addLiteral(SpdxProperties.LICENSE_ID, spdxId);
		resource.addLiteral(SpdxProperties.LICENSE_EXTRACTED_TEXT, text);
		return resource;
	}
}
