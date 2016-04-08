package com.yevster.spdxtra;

import org.apache.jena.rdf.model.Resource;

import com.yevster.spdxtra.model.SpdxDocument;
import com.yevster.spdxtra.model.SpdxElement;
import com.yevster.spdxtra.model.SpdxFile;
import com.yevster.spdxtra.model.SpdxPackage;

/**
 * This class contains all logic to generate instances of SPDX model objects
 * from RDF resources without static expectation of their types.
 * 
 * @author yevster
 *
 */
public class SpdxElementFactory {

	public static SpdxElement relationshipTargetFromResource(Resource r) {
		String rdfType = r.getProperty(SpdxProperties.RDF_TYPE).getObject().asResource().getURI();
		switch (rdfType) {
		case (SpdxPackage.RDF_TYPE):
			return new SpdxPackage(r);
		case (SpdxFile.RDF_TYPE):
			return new SpdxFile(r);
		case (SpdxDocument.RDF_TYPE):
			return new SpdxDocument(r);
		default:
			throw new IllegalArgumentException("Uneable to create SDPX element of type " + rdfType);
		}
	}

}
