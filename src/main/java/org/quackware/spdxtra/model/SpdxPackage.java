package org.quackware.spdxtra.model;

import java.util.Optional;

import org.apache.jena.ext.com.google.common.base.MoreObjects;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.quackware.spdxtra.SpdxUris;
import org.quackware.spdxtra.NoneNoAssertionOrValue;
import org.quackware.spdxtra.RdfResourceUpdate;

public class SpdxPackage extends SpdxElement implements SpdxIdentifiable{

	private static final Property PACKAGE_NAME_PROPERTY = new PropertyImpl(SpdxUris.SPDX_TERMS + "name");
	public static final String RDF_TYPE = SpdxUris.SPDX_TERMS + "Package";

	public SpdxPackage(Resource resource) {
		super(resource);
	}

	/**
	 * Returns the name of this package.
	 * 
	 * @return
	 */
	public String getName() {
		return getPropertyAsString(PACKAGE_NAME_PROPERTY);
	}
	
	/**
	 * Generates an RDF update for the package name.
	 * Causes no change to any data inside pkg.
	 * @param pkg
	 * @param newName
	 * @return
	 */
	public static RdfResourceUpdate updatePackageName(SpdxPackage pkg, String newName){
		return RdfResourceUpdate.updateStringProperty(pkg.getUri(), PACKAGE_NAME_PROPERTY, newName);
	}

	/**
	 * Returns the SPDX version info, if present in the document.
	 * 
	 * @return
	 */
	public Optional<String> getVersionInfo() {
		return Optional.ofNullable(getPropertyAsString(SpdxUris.SPDX_TERMS + "versionInfo"));
	}

	/**
	 * Returns the copyright text for this package.
	 * 
	 * @return
	 */
	public NoneNoAssertionOrValue getCopyright() {
		return getPropertyAsNoneNoAssertionOrValue(SpdxUris.SPDX_TERMS + "copyrightText");

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
