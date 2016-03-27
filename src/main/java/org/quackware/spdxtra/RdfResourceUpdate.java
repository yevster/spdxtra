package org.quackware.spdxtra;

import java.util.Objects;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.quackware.spdxtra.ModelDataAccess.IllegalUpdateException;

public class RdfResourceUpdate {
	private String resourceUri;
	private Property property;
	private UpdateRdfNodeBuilder newValueBuilder;
	private boolean createNewResouce = true;

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
		return new RdfResourceUpdate(resourceUri, property, updateBuilder);
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
	 *            created if it doesn't already exist. If set to false and the resource at resourceUri
	 *            doesn't exist at the time when the update is applied, a {@link IllegalUpdateException} will be thrown.
	 * @param property
	 *            The property to be updated
	 */
	public RdfResourceUpdate(String resourceUri, Property property, UpdateRdfNodeBuilder newValueBuilder) {
		this.resourceUri = Objects.requireNonNull(resourceUri);
		this.property = Objects.requireNonNull(property);
		this.newValueBuilder = Objects.requireNonNull(newValueBuilder);
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

	public boolean getCreateNewResource(){
		return createNewResouce;
	}

}
