package com.yevster.spdxtra;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * A collection of validation methods, all of which return true if their inputs
 * are valid.
 * 
 * @author yevster
 *
 */
public class Validate {

	private static final Pattern emailPattern = Pattern.compile(
			"^[A-Za-z0-9](([_\\.\\-]?[a-zA-Z0-9]+)*)@([A-Za-z0-9]+)(([\\.\\-]?[a-zA-Z0-9]+)*)\\.([A-Za-z]{2,})$");

	public static boolean spdxId(String spdxId) {
		return StringUtils.isNotBlank(spdxId) && !StringUtils.containsAny(spdxId, '#', ':')
				&& StringUtils.startsWith(spdxId, "SPDXRef-");

	}
	
	public static boolean spdxLicenseId(String spdxId) {
		return StringUtils.isNotBlank(spdxId) && !StringUtils.containsAny(spdxId, '#', ':')
				&& StringUtils.startsWith(spdxId, "LicenseRef-");

	}


	public static boolean name(String name) {
		return StringUtils.isNotBlank(name);
	}

	public static boolean email(String email) {
		return emailPattern.matcher(email).matches();
	}

	public static boolean baseUrl(String baseUrl) {
		return (StringUtils.isNotBlank(baseUrl) && !StringUtils.containsAny(baseUrl, '#'));
	}
}
