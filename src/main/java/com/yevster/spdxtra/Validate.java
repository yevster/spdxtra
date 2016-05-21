package com.yevster.spdxtra;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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

	private static final Function<String, ? extends RuntimeException> exceptionFactory = IllegalArgumentException::new;
	private static final BiFunction<String, Throwable, ? extends RuntimeException> exceptionFactoryWithCause = IllegalArgumentException::new;

	private static final Pattern emailPattern = Pattern.compile(
			"^[A-Za-z0-9](([_\\.\\-]?[a-zA-Z0-9]+)*)@([A-Za-z0-9]+)(([\\.\\-]?[a-zA-Z0-9]+)*)\\.([A-Za-z]{2,})$");

	public static void spdxLicenseId(String spdxId) {
		boolean valid = StringUtils.isNotBlank(spdxId) && !StringUtils.containsAny(spdxId, '#', ':')
				&& StringUtils.startsWith(spdxId, "LicenseRef-");
		if (!valid)
			throw exceptionFactory.apply(spdxId + " is not a valid SPDX License ID.");
	}

	public static void spdxElementId(String spdxId) {
		boolean valid = StringUtils.isNotBlank(spdxId) && !StringUtils.containsAny(spdxId, '#', ':')
				&& StringUtils.startsWith(spdxId, "SPDXRef-");
		if (!valid)
			throw exceptionFactory.apply(spdxId + " is not a valid SPDX Element ID.");
	}

	/**
	 * Fails when, and only when, validation is false
	 * 
	 * @param validation
	 *            Boolean expression that must be true (fails if false)
	 * @param message
	 *            The message to be included in exception on failure.
	 */
	public static void validate(boolean validation, String message) {
		if (!validation)
			throw exceptionFactory.apply(message);
	}

	public static void notNull(Object o) {
		if (o == null)
			throw exceptionFactory.apply("Argument may not be null");
	}

	public static void noNulls(Object[] array) {
		for (Object o : array) {
			Objects.requireNonNull(o);
		}
	}

	public static void notBlank(String name) {
		if (StringUtils.isBlank(name)) {
			throw exceptionFactory.apply("Argument cannot be blank");
		}
	}

	public static void email(String email) {
		boolean valid = emailPattern.matcher(email).matches();
		if (!valid)
			throw exceptionFactory.apply("Invalid email: " + email);
	}

	public static void baseUrl(String baseUrl) {
		boolean valid = (StringUtils.isNotBlank(baseUrl) && !StringUtils.containsAny(baseUrl, '#'));
		if (!valid)
			throw exceptionFactory.apply("Illegal namespace URL :" + baseUrl);
	}

	public static void spdxElementUri(String uri) {
		String[] elements = StringUtils.split(uri, "#", 2);

		if (elements.length != 2)
			throw exceptionFactory.apply("Illegal SPDX Element URI: " + uri);
		try {
			baseUrl(elements[0]);
			spdxElementId(elements[1]);
		} catch (RuntimeException e) {
			throw exceptionFactoryWithCause.apply("Illegal SPDX Element URI: " + uri, e);
		}

	}
}
