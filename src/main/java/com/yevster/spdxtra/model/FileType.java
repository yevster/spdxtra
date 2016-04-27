package com.yevster.spdxtra.model;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.yevster.spdxtra.SpdxUris;

public enum FileType {
	SOURCE, BINARY, ARCHIVE, APPLICATION, AUDIO, IMAGE, TEXT, VIDEO, DOCUMENTATION, SPDX, OTHER;

	public String getUri() {
		StringBuilder result = new StringBuilder(SpdxUris.SPDX_TERMS);
		result.append("fileType_");
		String[] elements = StringUtils.split(this.name(), '_');
		assert (elements.length >= 1);
		result.append(StringUtils.lowerCase(elements[0]));
		for (int i = 1; i < elements.length; ++i) {
			result.append(StringUtils.capitalize(StringUtils.lowerCase(elements[i])));
		}
		return result.toString();

	}

	/**
	 * Instantiates a relationship based on the relationships SPDX URI or local
	 * name from said URI.
	 * 
	 * @param uriOrLocalName
	 * @return
	 */
	public static FileType fromUri(String uriOrLocalName) {
		Objects.requireNonNull(uriOrLocalName);
		String localName = StringUtils.removeStart(uriOrLocalName, SpdxUris.SPDX_TERMS);
		localName = StringUtils.removeStart(localName, "fileType_");
		String[] tokens = StringUtils.splitByCharacterTypeCamelCase(localName);
		String result = StringUtils.join(tokens, "_");
		result = StringUtils.upperCase(result);
		return FileType.valueOf(result);

	}

}
