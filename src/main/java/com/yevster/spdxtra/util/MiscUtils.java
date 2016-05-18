package com.yevster.spdxtra.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;

public class MiscUtils {

	public static <T> Stream<T> toLinearStream(Iterator<T> iterator) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.NONNULL), false);
	}

	/**
	 * Returns an optional value that is empty if, and only if, the string is
	 * null or consists entirely of whitespace.
	 * 
	 * @param s
	 * @return
	 */
	public static Optional<String> optionalOfBlankable(String s) {
		return StringUtils.isBlank(s) ? Optional.empty() : Optional.of(s);
	}
}
