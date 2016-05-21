package com.yevster.spdxtra.model.write;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
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

	private static final Resource extractedLicenseType = ResourceFactory
			.createResource(SpdxUris.SPDX_TERMS + "ExtractedLicensingInfo");

	private String text;

	private String spdxId;

	private String baseUrl;

	private String name;

	private Optional<String> comment;

	public ExtractedLicense(String text, String name, String baseUrl, String spdxId, Optional<String> comment) {
		Validate.spdxLicenseId(spdxId);
		Validate.baseUrl(baseUrl);
		Validate.notBlank(text);
		this.spdxId = spdxId;
		this.text = text;
		this.baseUrl = baseUrl;
		this.name = name;
		this.comment = comment;

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
		resource.addLiteral(SpdxProperties.NAME, name);
		if (comment.isPresent()) {
			resource.addLiteral(SpdxProperties.RDF_COMMENT, comment.get());
		}
		return resource;
	}

	@Override
	public String getPrettyName() {
		return this.name;
	}
}
