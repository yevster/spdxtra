package com.yevster.spdxtra.model;

import java.util.Optional;


import org.apache.commons.lang3.StringUtils;


import com.google.common.base.MoreObjects;
import com.yevster.spdxtra.NoneNoAssertionOrValue;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;

import org.apache.jena.rdf.model.Resource;

public class SpdxPackage extends SpdxElement implements SpdxIdentifiable {

	public static final String RDF_TYPE = SpdxUris.SPDX_PACKAGE;

	public SpdxPackage(Resource resource) {
		super(resource);
	}

	/**
	 * Returns the name of this package.
	 * 
	 * @return
	 */
	public String getName() {
		return getPropertyAsString(SpdxProperties.SPDX_NAME);
	}
	
	/**
	 * Returns true if, and only if, the file contents of the package have been analyzed
	 * in preparing the SPDX document.
	 */
	public boolean getFilesAnalyzed(){
		String filesAnalyzedStr = getPropertyAsString(SpdxProperties.FILES_ANALYZED);
		return StringUtils.isBlank(filesAnalyzedStr) ? true : Boolean.parseBoolean(filesAnalyzedStr);
	}

	/**
	 * Returns the SPDX version info, if present in the document.
	 * 
	 * @return
	 */
	public Optional<String> getVersionInfo() {
		return Optional.ofNullable(getPropertyAsString(SpdxProperties.PACKAGE_VERSION_INFO));
	}

	/**
	 * Returns the copyright text for this package.
	 * 
	 * @return
	 */
	public NoneNoAssertionOrValue getCopyright() {
		return getPropertyAsNoneNoAssertionOrValue(SpdxProperties.COPYRIGHT_TEXT);

	}

	/**
	 * Returns the version of this package, if one is specified.
	 */

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SpdxPackage.class).add("SPDX ID", getSpdxId()).add("Name", getName())
				.add("Version", getVersionInfo().orElse("")).toString();
	}

}
