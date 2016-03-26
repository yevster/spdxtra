package org.quackware.spdxtra.model;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.base.MoreObjects;
import org.apache.jena.rdf.model.Resource;
import org.quackware.spdxtra.Namespaces;
import org.quackware.spdxtra.RdfResourceRepresentation;
import org.quackware.spdxtra.SpdxElementFactory;

public class Relationship extends RdfResourceRepresentation {
	public enum Type {
		DESCRIBES, DESCRIBED_BY, CONTAINS, CONTAINED_BY, GENERATES, GENERATED_FROM, ANCESTOR_OF, DESCENDANT_OF, VARIANT_OF, DISTRIBUTION_ARTIFACT, PATCH_FOR, PATCH_APPLIED, COPY_OF, FILE_ADDED, FILE_DELETED, FILE_MODIFIED, EXPANDED_FROM_ARCHIVE, DYNAMIC_LINK, STATIC_LINK, DATA_FILE, TESTCASE_OF, BUILD_TOOL_OF, DOCUMENTATION_OF, OPTIONAL_COMPONENT_OF, METAFILE_OF, PACKAGE_OF, AMENDS, PREREQUISITE_FOR, HAS_PREREQUISITE, OTHER;

		public String getUri() {
			StringBuilder result = new StringBuilder(Namespaces.SPDX_TERMS);
			result.append("relationshipType_");
			String[] elements = StringUtils.split(this.name(), '_');
			assert (elements.length >= 1);
			result.append(StringUtils.lowerCase(elements[0]));
			for (int i = 1; i < elements.length; ++i) {
				result.append(StringUtils.capitalize(StringUtils.lowerCase(elements[i])));
			}
			return result.toString();

		}

		/**
		 * Instantiates a relationship based on the relationships SPDX URI or
		 * local name from said URI.
		 * 
		 * @param uriOrLocalName
		 * @return
		 */
		public static Type fromUri(String uriOrLocalName) {
			Objects.requireNonNull(uriOrLocalName);
			String localName = StringUtils.removeStart(uriOrLocalName, Namespaces.SPDX_TERMS);
			localName = StringUtils.removeStart(localName, "relationshipType_");
			String[] tokens = StringUtils.splitByCharacterTypeCamelCase(localName);
			String result = StringUtils.join(tokens, "_");
			result = StringUtils.upperCase(result);
			return Type.valueOf(result);

		}
	}

	Optional<SpdxElement> relatedElement = Optional.empty();

	public Relationship(Resource r) {
		super(r);
	}

	public Type getType() {
		String uri = getPropertyAsResource(Namespaces.SPDX_TERMS + "relationshipType").getURI();
		return Type.fromUri(uri);
	}

	public SpdxElement getRelatedElement() {
		if (relatedElement.isPresent())
			return relatedElement.get();
		else {
			Resource r = getPropertyAsResource(Namespaces.SPDX_TERMS + "relatedSpdxElement");
			relatedElement = Optional.of(SpdxElementFactory.relationshipTargetFromResource(r));
			return relatedElement.get();
		}
	}

	@Override
	public String toString() {
		return new StringBuilder().append("[").append(getType()).append("](").append(getRelatedElement().getClass().getSimpleName())
				.append(")").append(getRelatedElement().getUri()).toString();
	}

}
