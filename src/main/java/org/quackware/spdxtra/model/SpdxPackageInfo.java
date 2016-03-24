package org.quackware.spdxtra.model;

import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.base.MoreObjects;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.quackware.spdxtra.Namespaces;
import org.quackware.spdxtra.NoneNoAssertionOrValue;
import org.quackware.spdxtra.RdfResourceRepresentation;
import org.quackware.spdxtra.util.MiscUtils;

public class SpdxPackageInfo extends RdfResourceRepresentation {
	
	
	public SpdxPackageInfo(Resource resource) {
		super(resource);
	}

	/**
	 * Returns the name of this package.
	 * 
	 * @return
	 */
	public String getName() {
		return getPropertyAsString(Namespaces.SPDX_TERMS + "name");
	}

	/**
	 * Returns the SPDX Identifier of this package, provided one is specified as
	 * part of the package's URI, per Section 3.3 of the SPDX 2.0 Specification.
	 */
	public String getSpdxId() {
		String uri = getUri();
		return StringUtils.substringAfterLast(uri, "#");
	}

	/**
	 * Returns the SPDX version info, if present in the document.
	 * 
	 * @return
	 */
	public Optional<String> getVersionInfo() {
		return Optional.ofNullable(getPropertyAsString(Namespaces.SPDX_TERMS + "versionInfo"));
	}

	/**
	 * Returns the copyright text for this package.
	 * 
	 * @return
	 */
	public NoneNoAssertionOrValue getCopyright() {
		return getPropertyAsNoneNoAssertionOrValue(Namespaces.SPDX_TERMS + "copyrightText");
		
	}

	/**
	 * Returns a supplier of SpdxFileInfo. Supplier will (lazily) return a file
	 * when get() is invoked, and will return Null if there are no files left.
	 * 
	 * @return
	 */
	public Iterable<SpdxFileInfo> getFiles() {
		Resource hasFileResource = getPropertyAsResource(Namespaces.SPDX_TERMS + "hasFile");
		final StmtIterator it = hasFileResource.listProperties();

		return MiscUtils.fromIteratorConsumer(it, (s)->{
				String uri = s.getSubject().getURI();
				return new SpdxFileInfo(hasFileResource.getModel().getResource(uri));
		});

	}

	/**
	 * Returns the version of this package, if one is specified.
	 */

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SpdxPackageInfo.class).add("SPDX ID", getSpdxId()).add("Name", getName())
				.add("Version", getVersionInfo().orElse("")).toString();
	}

}
