package com.yevster.spdxtra;

import java.time.format.DateTimeFormatter;

public class Constants {
	public static final String DEFAULT_SPDX_VERSION = "SPDX-2.1";
	// Because DateTimeFormatter.ISO_INSTANT doesn't parse the 'Z'
	public static final DateTimeFormatter SPDX_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
	public static final String LICENSE_LIST_LOCATION_PROPERTY = "spdxtra.license.list.location";
	public static final String LICENSE_LIST_URL = "http://spdx.org/licenses/";
}
