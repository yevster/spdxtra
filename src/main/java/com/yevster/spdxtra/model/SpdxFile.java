package com.yevster.spdxtra.model;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.base.MoreObjects;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import com.yevster.spdxtra.NoneNoAssertionOrValue;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;
import com.yevster.spdxtra.util.MiscUtils;

public class SpdxFile extends SpdxElement implements SpdxIdentifiable {
	public static final String RDF_TYPE = SpdxUris.SPDX_TERMS + "File";

	public SpdxFile(Resource resource) {
		super(resource);
		String resourceType = resource.getProperty(SpdxProperties.RDF_TYPE).getObject().asResource().getURI();
		if (!StringUtils.equals(resourceType, SpdxUris.SPDX_FILE)) {
			throw new IllegalArgumentException("Resource " + resource.getURI() + " is not an SPDX file");
		}
	}

	public String getFileName() {
		return getPropertyAsString(SpdxUris.SPDX_TERMS + "fileName");
	}

	public Set<FileType> getFileTypes() {
		Set<FileType> result = 
				MiscUtils.toLinearStream(this.rdfResource.listProperties(SpdxProperties.FILE_TYPE))
					.map(Statement::getResource)
					.map(Resource::getURI)
					.map(FileType::fromUri)
					.collect(Collectors.toSet());

		return Collections.unmodifiableSet(result);
	}

	public Set<Checksum> getChecksums() {
		Set<Checksum> result = 
				MiscUtils.toLinearStream(this.rdfResource.listProperties(SpdxProperties.CHECKSUM))
						.map(Statement::getResource)
						.map(Checksum::fromResource)
						.collect(Collectors.toSet());
		return Collections.unmodifiableSet(result);
	}
	
	/**
	 * Returns all the contributors defined on this file.
	 */
	public Set<String> getContributors(){
		Set<String> result = 
				MiscUtils.toLinearStream(this.rdfResource.listProperties(SpdxProperties.FILE_CONTRIBUTOR))
				.map(Statement::getObject)
				.map(RDFNode::asLiteral)
				.map(Literal::getString)
				.collect(Collectors.toSet());
		return Collections.unmodifiableSet(result);
	}
	/**
	 * @return
	 */
	public Optional<String> getComment(){
		Statement stmt =  rdfResource.getProperty(SpdxProperties.RDF_COMMENT);
		if (stmt == null) return Optional.empty();
		return Optional.of(stmt.getObject().asLiteral().getString());
	}

	public NoneNoAssertionOrValue getCopyrightText() {
		return NoneNoAssertionOrValue.parse(rdfResource.getProperty(SpdxProperties.COPYRIGHT_TEXT).getString());
	}

	/**
	 * Returns the file's notice text, if present.
	 */
	public Optional<String> getNoticeText(){
		return getOptionalPropertyAsString(SpdxProperties.NOTICE_TEXT);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SpdxPackage.class).add("File name", getFileName()).toString();
	}
}
