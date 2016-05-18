package com.yevster.spdxtra.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;

import com.yevster.spdxtra.Constants;
import com.yevster.spdxtra.RdfResourceRepresentation;
import com.yevster.spdxtra.RdfResourceUpdate;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.SpdxUris;

/**
 * Describes an SPDX document.
 *
 */
public class SpdxDocument extends SpdxElement implements SpdxIdentifiable {
	public static final String RDF_TYPE = SpdxUris.SPDX_DOCUMENT;

	public static class CreationInfo extends RdfResourceRepresentation {
		CreationInfo(Resource r) {
			super(r);
		}
		
		/**
		 * Returns the UTC creation date time.
		 */
		public ZonedDateTime getCreationDate(){
			String creationDate = rdfResource.getProperty(SpdxProperties.CREATION_DATE).getString();
			return ZonedDateTime.of(LocalDateTime.parse(creationDate, Constants.SPDX_DATE_FORMATTER), ZoneId.of("UTC"));
		}
		
		/**
		 * Returns the creation comment
		 */
		public Optional<String> getComment(){
			return getOptionalPropertyAsString(SpdxProperties.RDF_COMMENT);
		}
	}

	public SpdxDocument(Resource resource) {
		super(resource);
	}

	public String getName() {
		return getPropertyAsString(SpdxProperties.SPDX_NAME);
	}

	public String getDocumentNamespace() {
		return StringUtils.substringBeforeLast(getUri(), "#");
	}
	
	/**
	 * Returns the document comment
	 * @return
	 */
	public Optional<String> getComment(){
		return getOptionalPropertyAsString(SpdxProperties.RDF_COMMENT);
	}

	/**
	 * Returns the version of the SPDX specification used to build the document.
	 * 
	 * @return
	 */
	public String getSpecVersion() {
		return getPropertyAsString(SpdxProperties.SPEC_VERSION);
	}


	public CreationInfo getCreationInfo(){
		return new CreationInfo(getPropertyAsResource(SpdxProperties.CREATION_INFO).get());
	}

	

	
	/* Addition generators */

}
