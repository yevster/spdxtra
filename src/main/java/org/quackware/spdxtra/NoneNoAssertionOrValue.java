package org.quackware.spdxtra;

import java.util.Objects;
import java.util.Optional;

public class NoneNoAssertionOrValue{
	public static final NoneNoAssertionOrValue NO_ASSERTION = new NoneNoAssertionOrValue(AbsentValue.NOASSERTION);
	
	public static final NoneNoAssertionOrValue NONE = new NoneNoAssertionOrValue(AbsentValue.NONE);

	
 	private Optional<String> value = Optional.empty();
 	private Optional<AbsentValue> absentValue = Optional.empty();
	public enum AbsentValue{NONE, NOASSERTION}
	
	public static NoneNoAssertionOrValue of(String value){
		return new NoneNoAssertionOrValue(value);
	}
	
	private NoneNoAssertionOrValue(String value){
		this.value = Optional.of(value);
	}
	
	private NoneNoAssertionOrValue(AbsentValue value){
		this.absentValue = Optional.of(value);
	}
	
 
	
	public Optional<AbsentValue> getAbsentValue(){
		return absentValue;
	}
	
	public Optional<String> getValue() {
		return value;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		return obj != null && obj instanceof NoneNoAssertionOrValue
				&& Objects.equals(getAbsentValue(), ((NoneNoAssertionOrValue)obj).getAbsentValue())
				&& Objects.equals(getValue(), ((NoneNoAssertionOrValue)obj).getValue());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(getValue(), getAbsentValue());
	}
}