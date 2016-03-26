package org.quackware.spdxtra.model;

import org.apache.commons.lang3.StringUtils;

public interface SpdxIdentifiable {
	
	public String getUri();
	
	/**
	 * Returns the SPDX Identifier of this package, provided one is specified as
	 * part of the package's URI, per Section 3.3 of the SPDX 2.0 Specification.
	 */
	default String getSpdxId() {
		String uri = getUri();
		return StringUtils.substringAfterLast(uri, "#");
	}
}
