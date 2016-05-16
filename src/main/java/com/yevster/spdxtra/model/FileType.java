package com.yevster.spdxtra.model;

public enum FileType {
	SOURCE, BINARY, ARCHIVE, APPLICATION, AUDIO, IMAGE, TEXT, VIDEO, DOCUMENTATION, SPDX, OTHER;

	private static final String URI_PREFIX = "fileType_";

	public String getUri() {
		return ResourceEnumUtils.toUri(this, URI_PREFIX);
	}

	/**
	 * Instantiates a relationship based on the relationships SPDX URI or local
	 * name from said URI.
	 * 
	 * @param uriOrLocalName
	 * @return
	 */
	public static FileType fromUri(String uriOrLocalName) {
		return ResourceEnumUtils.enumFromUri(FileType.class, uriOrLocalName, URI_PREFIX);

	}

}
