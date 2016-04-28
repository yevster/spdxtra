package com.yevster.spdxtra.model;

import com.github.andrewoma.dexx.collection.Sets;
import com.google.common.base.MoreObjects;
import com.yevster.spdxtra.NoneNoAssertionOrValue;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;
import com.yevster.spdxtra.util.MiscUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

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
	
	public Set<FileType> getFileTypes(){
		Set<FileType> result = 
				MiscUtils.toLinearStream(this.rdfResource.listProperties(SpdxProperties.FILE_TYPE))
					.map(Statement::getResource)
					.map(Resource::getURI)
					.map(FileType::fromUri)
					.collect(Collectors.toSet());
					
		return Collections.unmodifiableSet(result);
	}

	public Set<Checksum> getChecksums(){
		Set<Checksum> result = 
				MiscUtils.toLinearStream(this.rdfResource.listProperties(SpdxProperties.CHECKSUM))
						.map(Statement::getResource)
						.map(Checksum::fromResource)
						.collect(Collectors.toSet());
		return Collections.unmodifiableSet(result);
	}
	
	public NoneNoAssertionOrValue getCopyrightText(){
		return NoneNoAssertionOrValue.parse(rdfResource.getProperty(SpdxProperties.COPYRIGHT_TEXT).getString());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SpdxPackage.class).add("File name", getFileName()).toString();
	}
}
