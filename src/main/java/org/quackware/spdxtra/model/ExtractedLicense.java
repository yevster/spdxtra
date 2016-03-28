package org.quackware.spdxtra.model;

import org.apache.jena.rdf.model.Resource;

/**
 * Represents an SPDX extracted license (i.e. a license whose full text is obtained from a file)
 * @author ybronshteyn
 *
 */
public class ExtractedLicense extends License{
	public ExtractedLicense(Resource r){
		super(r);
	}
}
