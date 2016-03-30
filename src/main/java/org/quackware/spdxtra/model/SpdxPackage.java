package org.quackware.spdxtra.model;

import java.util.Optional;

import org.apache.jena.ext.com.google.common.base.MoreObjects;
import org.apache.jena.rdf.model.Resource;
import org.quackware.spdxtra.SpdxUris;
import org.quackware.spdxtra.NoneNoAssertionOrValue;
import org.quackware.spdxtra.SpdxProperties;

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
