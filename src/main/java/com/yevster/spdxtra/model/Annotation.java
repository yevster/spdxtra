package com.yevster.spdxtra.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import com.yevster.spdxtra.Constants;
import com.yevster.spdxtra.RdfResourceRepresentation;
import com.yevster.spdxtra.SpdxProperties;

public class Annotation extends RdfResourceRepresentation {
	public static enum Type {
		REVIEW, OTHER;

		private static final String PREFIX = "annotationType_";

		public String getUri() {
			return ResourceEnumUtils.toUri(this, PREFIX);
		}

		public static Type fromUri(String uriOrLocalName) {
			return ResourceEnumUtils.enumFromUri(Type.class, uriOrLocalName, PREFIX);
		}

	}

	public Annotation(Resource resource) {
		super(resource);
	}

	/**
	 * Returns the annotation's type
	 * 
	 * @return
	 */
	public Type getType() {
		String uri = rdfResource.getRequiredProperty(SpdxProperties.ANNOTATION_TYPE).getResource().getURI();
		return Type.fromUri(uri);
	}

	public Optional<String> getComment() {
		Statement comment = rdfResource.getProperty(SpdxProperties.RDF_COMMENT);
		if (comment == null)
			return Optional.empty();
		return Optional.of(comment.getObject().asLiteral().getString());
	}

	public ZonedDateTime getDate() {
		// SPDX Dates are always in ISO-8601 UTC.
		LocalDateTime ldt = LocalDateTime.parse(getPropertyAsString(SpdxProperties.ANNOTATION_DATE), Constants.SPDX_DATE_FORMATTER);
		return ZonedDateTime.of(ldt, ZoneId.of("UTC"));
	}

	public Creator getAnnotator() {
		return Creator.fromString(getPropertyAsString(SpdxProperties.ANNOTATOR));
	}

}
