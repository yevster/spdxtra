package org.quackware.spdxtra.model;

import org.apache.commons.lang3.StringUtils;
import org.quackware.spdxtra.Namespaces;

public enum Relationship {
	DESCRIBES,
	DESCRIBED_BY,
	CONTAINS,
	CONTAINED_BY,
	GENERATES,
	GENERATED_FROM,
	ANCESTOR_OF,
	DESCENDANT_OF,
	VARIANT_OF,
	DISTRIBUTION_ARTIFACT,
	PATCH_FOR,
	PATCH_APPLIED,
	COPY_OF,
	FILE_ADDED,
	FILE_DELETED,
	FILE_MODIFIED,
	EXPANDED_FROM_ARCHIVE,
	DYNAMIC_LINK,
	STATIC_LINK,
	DATA_FILE,
	TESTCASE_OF,
	BUILD_TOOL_OF,
	DOCUMENTATION_OF,
	OPTIONAL_COMPONENT_OF,
	METAFILE_OF,
	PACKAGE_OF,
	AMENDS,
	PREREQUISITE_FOR,
	HAS_PREREQUISITE,
	OTHER;
	
	public String getUri(){
		StringBuilder result = new StringBuilder(Namespaces.SPDX_TERMS);
		result.append("relationshipType_");
		String[] elements = StringUtils.split(this.name(), '_');
		assert(elements.length>=1);
		result.append(StringUtils.lowerCase(elements[0]));
		for (int i=1; i<elements.length;++i){
			result.append(StringUtils.capitalize(StringUtils.lowerCase(elements[i])));
		}
		return result.toString();
		
	}
}
