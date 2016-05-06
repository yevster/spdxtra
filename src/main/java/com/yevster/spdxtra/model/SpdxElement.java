package com.yevster.spdxtra.model;

import org.apache.jena.rdf.model.Resource;

import com.yevster.spdxtra.RdfResourceRepresentation;

/**
 * Represents an SPDX element that can be the source and/or target of SPDX
 * relationships.
 * 
 * @author yevster
 *
 */
public abstract class SpdxElement extends RdfResourceRepresentation {
	public SpdxElement(Resource r) {
		super(r);
	}

}
