package com.yevster.spdxtra.model;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 * Represents an SPDX extracted license (i.e. a license whose full text is
 * obtained from a file)
 * 
 * @author ybronshteyn
 *
 */
public class ExtractedLicense extends License {
	// PLACEHOLDER.
	// TODO: Implement this.
	private final Resource resource;

	public ExtractedLicense(Resource r) {
		this.resource = r;
	}

	@Override
	public RDFNode getRdfNode() {
		return resource;
	}
}
