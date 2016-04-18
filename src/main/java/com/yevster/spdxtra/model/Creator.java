package com.yevster.spdxtra.model;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

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

}
