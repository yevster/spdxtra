package com.yevster.spdxtra;

import java.util.Objects;
import java.util.Optional;

/**
 * This class is designed to store values for SPDX properties where the
 * acceptable values are NONE, NOASSERTION, or some user-defined literal. 
 * 
 * @author yevster
 *
 */
public class NoneNoAssertionOrValue {
	public static final NoneNoAssertionOrValue NO_ASSERTION = new NoneNoAssertionOrValue(AbsentValue.NOASSERTION);

	public static final NoneNoAssertionOrValue NONE = new NoneNoAssertionOrValue(AbsentValue.NONE);

	private Optional<String> value = Optional.empty();
	private Optional<AbsentValue> absentValue = Optional.empty();

	public enum AbsentValue {
		NONE(SpdxUris.SPDX_TERMS + "none"), NOASSERTION(SpdxUris.SPDX_TERMS + "noassertion");
		private final String uri;

		private AbsentValue(String uri) {
			this.uri = uri;
		}

		public String getUri() {
			return uri;
		}
	}

	public static NoneNoAssertionOrValue of(String value) {
		return new NoneNoAssertionOrValue(value);
	}

    /**
     * If the argument string corresponds to a URI for NONE or NO_ASSERTION,
     * returns that value. Otherwise, returns the object with the literal value of the string.
     *
     * @param toParse
     * @return
     */
    public static NoneNoAssertionOrValue parse(String toParse) {
        if (AbsentValue.NOASSERTION.getUri().equals(toParse)) {
            return NO_ASSERTION;
        } else if (AbsentValue.NONE.getUri().equals(toParse)) {
            return NONE;
        } else return NoneNoAssertionOrValue.of(toParse);
    }

    private NoneNoAssertionOrValue(String value) {
        this.value = Optional.of(value);
    }

	private NoneNoAssertionOrValue(AbsentValue value) {
		this.absentValue = Optional.of(value);
	}

	/**
	 * If this instance is NONE or NOASSERTION, returns that value, otherwise
	 * empty.
	 * 
	 * @return
	 */
	public Optional<AbsentValue> getAbsentValue() {
		return absentValue;
	}

	/**
	 * If this instance is not NONE or NOASSERTION, returns the value to which
	 * it's set. Otherwise empty.
	 * 
	 * @return
	 */
	public Optional<String> getValue() {
		return value;
	}

	/**
	 * Returns the literal value. If this instance is NONE or NOASSERTION,
	 * returns the URI for that value. Otherwise, returns the set value.
	 * 
	 * @return
	 */
	public String getLiteralOrUriValue() {
		
		return getValue().isPresent() ? getValue().get() : getAbsentValue().get().getUri();
	}

	@Override
	public boolean equals(Object obj) {

		return obj != null && obj instanceof NoneNoAssertionOrValue
				&& Objects.equals(getAbsentValue(), ((NoneNoAssertionOrValue) obj).getAbsentValue())
				&& Objects.equals(getValue(), ((NoneNoAssertionOrValue) obj).getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getValue(), getAbsentValue());
	}
}