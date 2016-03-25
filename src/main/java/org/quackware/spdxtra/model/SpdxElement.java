package org.quackware.spdxtra.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;
import org.quackware.spdxtra.RdfResourceRepresentation;


/**
 * Represents a generic SPDX element with an SPDX ID.
 * @author yevster
 *
 */
public abstract class SpdxElement extends RdfResourceRepresentation implements Relatable{
	public SpdxElement(Resource r){
		super(r);
	}

	/**
	 * Returns the SPDX Identifier of this package, provided one is specified as
	 * part of the package's URI, per Section 3.3 of the SPDX 2.0 Specification.
	 */
	public String getSpdxId() {
		String uri = getUri();
		return StringUtils.substringAfterLast(uri, "#");
	}
}
