package org.quackware.spdxtra;

import org.apache.jena.rdf.model.Resource;
import org.quackware.spdxtra.model.Relatable;
import org.quackware.spdxtra.model.SpdxDocument;
import org.quackware.spdxtra.model.SpdxFile;
import org.quackware.spdxtra.model.SpdxPackage;

/**
 * This class contains all logic to generate instances of SPDX model objects
 * from RDF resources without static expectation of their types.
 * 
 * @author yevster
 *
 */
class SpdxElementFactory {

	static Relatable relationshipTargetFromResource(Resource r) {
		String rdfType = r.getProperty(RdfResourceRepresentation.RDF_TYPE).getObject().asResource().getURI();
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
