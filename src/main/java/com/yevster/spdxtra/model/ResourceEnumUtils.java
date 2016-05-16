package com.yevster.spdxtra.model;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.yevster.spdxtra.SpdxUris;

/**
 * Assists with enums whose elements can be represented with URIs.
 */
class ResourceEnumUtils {
	/**
	 * Converts enum values to URIs with the provided prefix. So an enum value
	 * MyEnum.ENUM_VALUE and prefix "fooBar_" would be converted to
	 * http://spdx.org/rdf/terms#fooBar_enumValue.
	 * 
	 * @param enumValue
	 * @param prefix A string that comes between the base SPDX URI and the enum value.
	 * If an underscore needs to be included at the end of the prefix, it should be included
	 * <strong>into</strong> the prefix.
	 * @return
	 */
	public static String toUri(Enum<?> enumValue, String prefix) {
		StringBuilder result = new StringBuilder(SpdxUris.SPDX_TERMS);
		result.append(prefix);
		String[] elements = StringUtils.split(enumValue.name(), '_');
		assert (elements.length >= 1);
		result.append(StringUtils.lowerCase(elements[0]));
		for (int i = 1; i < elements.length; ++i) {
			result.append(StringUtils.capitalize(StringUtils.lowerCase(elements[i])));
		}
		return result.toString();
	}

	/**
	 * Converts URIs of enumerated types to enum values
	 * @param clazz
	 * @param uriOrLocalName
	 * @param prefix
	 * @return
	 */
	public static <T extends Enum<T>> T enumFromUri(Class<T> clazz, String uriOrLocalName, String prefix) {
		Objects.requireNonNull(uriOrLocalName);
		String localName = StringUtils.removeStart(uriOrLocalName, SpdxUris.SPDX_TERMS);
		localName = StringUtils.removeStart(localName, prefix);
		String[] tokens = StringUtils.splitByCharacterTypeCamelCase(localName);
		String result = StringUtils.join(tokens, "_");
		result = StringUtils.upperCase(result);
		return Enum.valueOf(clazz, result);

	}

}
