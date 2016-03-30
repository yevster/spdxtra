package org.quackware.spdxtra;

import org.apache.commons.lang3.StringUtils;

public class Validate {
	public static boolean spdxId(String spdxId){
		return StringUtils.isNotBlank(spdxId) && !StringUtils.containsAny(spdxId, '#', ':') && StringUtils.startsWith(spdxId, "SPDXRef-");
			
	}
}
