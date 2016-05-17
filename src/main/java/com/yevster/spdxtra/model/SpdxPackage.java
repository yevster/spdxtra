package com.yevster.spdxtra.model;

import com.google.common.base.MoreObjects;
import com.yevster.spdxtra.NoneNoAssertionOrValue;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;
import com.yevster.spdxtra.util.MiscUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.Optional;
import java.util.stream.Stream;

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
	 * Returns true if, and only if, the file contents of the package have been
	 * analyzed in preparing the SPDX document.
	 */
	public boolean getFilesAnalyzed() {
		String filesAnalyzedStr = getPropertyAsString(SpdxProperties.FILES_ANALYZED);
		return StringUtils.isBlank(filesAnalyzedStr) || Boolean.parseBoolean(filesAnalyzedStr);
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
	 * Returns the file name for this package
	 */
	public Optional<String> getPackageFileName() {
		return getOptionalPropertyAsString(SpdxProperties.PACKAGE_FILE_NAME);
	}

	/**
	 * Returns the package download location for this package.
	 */
	public NoneNoAssertionOrValue getPackageDownloadLocation() {
		return getPropertyAsNoneNoAssertionOrValue(SpdxProperties.PACKAGE_DOWNLOAD_LOCATION);
	}

	/**
	 * Returns the summary description of the package, if present.
	 */
	public Optional<String> getSummary(){
		return getOptionalPropertyAsString(SpdxProperties.SUMMARY);
	}
	
	/**
	 * Returns the detailed description of the package, if present.
	 */
	public Optional<String> getDescription(){
		return getOptionalPropertyAsString(SpdxProperties.DESCRIPTION);
	}
	
	/**
	 * Returns the value of the source information field of the package, if present.
	 */
	public Optional<String> getSourceInfo(){
		return getOptionalPropertyAsString(SpdxProperties.SOURCE_INFO);
	}
	
	/**
	 * Returns the homepage of this package, if one is available. This is an
	 * optional element. If omitted, NOASSERTION will be returned.
	 */
	public NoneNoAssertionOrValue getHomepage() {
		return getPropertyAsNoneNoAssertionOrValue(SpdxProperties.HOMEPAGE);
	}
	
	/**
	 * Returns the package's comment, if one is present.
	 */
	public Optional<String> getComment(){
		return getOptionalPropertyAsString(SpdxProperties.RDF_COMMENT);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SpdxPackage.class).add("SPDX ID", getSpdxId()).add("Name", getName())
				.add("Version", getVersionInfo().orElse("")).toString();
	}

	public Stream<SpdxFile> getFiles() {
		Stream<Statement> fileStatementStream = MiscUtils.toLinearStream(this.rdfResource.listProperties(SpdxProperties.HAS_FILE));
		return fileStatementStream.map(Statement::getObject).map(RDFNode::asResource).map((r) -> new SpdxFile(r));
	}

	/**
	 * Returns the package verification code, if one is present (i.e.
	 * filesAnalyzed = true or omitted). If filesAnalyzed is false, returns
	 * empty.
	 * 
	 * @return
	 */
	public Optional<String> getPackageVerificationCode() {
		Optional<Resource> pvc = getPropertyAsResource(SpdxProperties.PACKAGE_VERIFICATION_CODE);
		if (!pvc.isPresent())
			return Optional.empty();
		return Optional.of(pvc.get().getProperty(SpdxProperties.PACKAGE_VERIFICATION_CODE_VALUE).getObject().asLiteral().getString());

	}

	
	
}
