package com.yevster.spdxtra.model;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.yevster.spdxtra.Validate;

public final class Creator {

	private Supplier<String> toString;

	private Creator(String name, Optional<String> email, Supplier<String> toString) {
		if (!Validate.name(name)) {
			throw new IllegalArgumentException("Inavlid name: " + name);
		}
		if (Objects.requireNonNull(email).isPresent() && !Validate.email(email.get())) {
			throw new IllegalArgumentException("Illegal email: " + email);
		}

		this.toString = toString;
	}

	public static Creator person(String name, Optional<String> email) {
		return new Creator(name, email, () -> "Person: " + name + " (" + email.orElse("") + ")");
	}

	public static Creator organization(String name, Optional<String> email) {
		return new Creator(name, email, () -> "Organization: " + name + " (" + email.orElse("") + ")");
	}

	public static Creator tool(String name) {
		return new Creator(name, Optional.empty(), () -> "Tool: " + name);
	}

	@Override
	public String toString() {
		return this.toString.get();
	}

	@Override
	public boolean equals(Object obj) {
		return obj != null && Objects.equals(toString(), obj.toString());
	}

	@Override
	public int hashCode() {
		return Objects.hash(toString());
	}

	public static Creator fromString(final String string) {
		String name = null;
		if (StringUtils.startsWith(string, "Tool:")) {
			name = StringUtils.trim(StringUtils.removeStart(string, "Tool: "));
			return Creator.tool(name);
		}

		final BiFunction<String, Optional<String>, Creator> creatorCreator;
		if (StringUtils.startsWith(string, "Person:")) {
			name = StringUtils.removeStart(string, "Person:");
			creatorCreator = Creator::person;
		} else if (StringUtils.startsWith(string, "Organization:")) {
			name = StringUtils.removeStart(string, "Organization:");
			creatorCreator = Creator::organization;
		} else {
			throw new IllegalArgumentException(string + " is not a valid SPDX creator.");
		}

		Optional<String> email = Optional.empty();
		if (name.contains("(")) {
			String emailString = StringUtils.trim(StringUtils.substringBeforeLast(StringUtils.substringAfterLast(name, "("), ")"));
			if (!StringUtils.isBlank(emailString)) {
				email = Optional.of(emailString);
			}
			name = StringUtils.substringBeforeLast(name, "(");
		}
		name = StringUtils.trim(name);
		return creatorCreator.apply(name, email);
	}

}
