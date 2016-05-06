package com.yevster.spdxtra;

public final class SpdxUris {

	public static final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String DOAP_NAMESPACE = "http://usefulinc.com/ns/doap#";
	public static final String SPDX_TERMS = "http://spdx.org/rdf/terms#";
	public static final String SPDX_RELATIONSHIP = SPDX_TERMS + "relationship";
	public static final String SPDX_DOCUMENT = SPDX_TERMS + "SpdxDocument";
	public static final String SPDX_PACKAGE = SpdxUris.SPDX_TERMS + "Package";
	public static final String SPDX_FILE = SpdxUris.SPDX_TERMS + "File";
	public static final String LISTED_LICENSE_NAMESPACE = "http://spdx.org/licenses/";
	public static final String NO_ASSERTION = SPDX_TERMS + "noassertion";
	public static final String NONE = SPDX_TERMS + "none";

}
