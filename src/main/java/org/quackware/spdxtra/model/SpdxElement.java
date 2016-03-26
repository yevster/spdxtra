package org.quackware.spdxtra.model;

import org.apache.jena.rdf.model.Resource;
import org.quackware.spdxtra.RdfResourceRepresentation;


/**
 * Represents an SPDX element that can be the source and/or target of SPDX relationships.
 * @author yevster
 *
 */
public abstract class SpdxElement extends RdfResourceRepresentation {
	public SpdxElement(Resource r){
		super(r);
	}


}
