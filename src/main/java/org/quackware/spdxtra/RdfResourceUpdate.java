package org.quackware.spdxtra;

import java.util.Objects;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.quackware.spdxtra.Read.IllegalUpdateException;

public class RdfResourceUpdate {
	private String resourceUri;
	private Property property;
	private UpdateRdfNodeBuilder newValueBuilder;
	private boolean createNewProperty = false;

	/**
	 * Creates an update that updates an existing user property.
	 * 
	 * @param resourceUri
	 * @param property
	 * @param updatedValue
	 * @return
	 */
	public static RdfResourceUpdate updateStringProperty(String resourceUri, Property property, String updatedValue) {
		UpdateRdfNodeBuilder updateBuilder = (Model model) -> model.createLiteral(updatedValue);
		return new RdfResourceUpdate(resourceUri, property, false, updateBuilder);
	}

	public interface UpdateRdfNodeBuilder {
		RDFNode newValue(Model model);
	}

	/**
	 * This constructor has been made public for extensibility, but you should
	 * use the static factory methods instead whenever possible. If your use
	 * case is not covered by one of the static factory methods, consider making
	 * a feature or pull request.
	 * 
	 * @param resourceUri
	 *            The URI of the resource to be updated
	 * @param createNewResource
	 *            True if, and only if, the resource at that URI should be
	 *            created if it doesn't already exist. If set to false and the
	 *            resource at resourceUri doesn't exist at the time when the
	 *            update is applied, a {@link IllegalUpdateException} will be
	 *            thrown.
	 * @param property
	 *            The property to be updated
	 * @param createNewProperty
	 *            Whether or not a new property should be created if the
	 *            property already exists. Should be true only if multiple
	 *            instances of one property could exist (e.g. one SPDX element
	 *            can have multiple relationships).
	 * 
	 */
	public RdfResourceUpdate(String resourceUri, Property property, boolean createNewProperty, UpdateRdfNodeBuilder newValueBuilder) {
		this.resourceUri = Objects.requireNonNull(resourceUri);
		this.property = Objects.requireNonNull(property);
		this.newValueBuilder = Objects.requireNonNull(newValueBuilder);
		this.createNewProperty = createNewProperty;
	}

	public String getResourceUri() {
		return resourceUri;
	}

	public Property getProperty() {
		return property;
	}

	public UpdateRdfNodeBuilder getNewValueBuilder() {
		return newValueBuilder;
	}

	public boolean getCreateNewProperty() {
		return createNewProperty;
	}

}
