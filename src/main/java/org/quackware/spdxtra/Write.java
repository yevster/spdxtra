package org.quackware.spdxtra;

import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.quackware.spdxtra.model.SpdxPackage;

public final class Write {
	/**
	 * Only useful when creating new resources, so this shouldn't be used anywhere else.
	 */
	private static final Property rdfTypeProperty = new PropertyImpl(SpdxUris.RDF_NAMESPACE, "type");
	public static final class New{
		/**
		 * Creates a dataset update that creates a document.
		 * @param baseUrl
		 * @param spdxId
		 * @return
		 */
		public static RdfResourceUpdate document(String baseUrl, String spdxId){
			//validation
			if (StringUtils.isBlank(baseUrl) ||
					StringUtils.containsAny(baseUrl, '#')){
				throw new IllegalArgumentException("Illegal base URL: "+baseUrl);
			}
			if (StringUtils.isBlank(spdxId)||
				StringUtils.containsAny(spdxId,'#',':')
				|| !StringUtils.startsWith(spdxId, "SPDXRef-")){
				throw new IllegalArgumentException("Illegal spdxId. Must be of the form SPDXRef-*");
			}
			String uri = baseUrl+"#"+spdxId;
			URI.create(uri); //An extra validity check.
			
			return RdfResourceUpdate.updateStringProperty(baseUrl, Write.rdfTypeProperty, SpdxUris.SPDX_DOCUMENT);
		}
	}
	
	public static final class Document{
		
	}
	public static final class Package{

		/**
		 * Generates an RDF update for the package name.
		 * Causes no change to any data inside pkg.
		 * @param pkg
		 * @param newName
		 * @return
		 */
		public static RdfResourceUpdate name(SpdxPackage pkg, String newName){
			return RdfResourceUpdate.updateStringProperty(pkg.getUri(), SpdxProperties.PACKAGE_NAME, newName);
		}
		
	}

	public static void applyUpdatesInOneTransaction(Dataset dataset, Iterable<RdfResourceUpdate> updates) {
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.WRITE);) {
			Model model = dataset.getDefaultModel();
			for (RdfResourceUpdate update : updates) {
		
				Resource resource = model.getResource(update.getResourceUri());
	
				if (update.getCreateNewProperty()) {
					resource.addProperty(update.getProperty(), update.getNewValueBuilder().newValue(model));
				} else {
					Statement s = resource.getProperty(update.getProperty());
					if (s != null) {
						s.changeObject(update.getNewValueBuilder().newValue(model));
					} else {
						resource.addProperty(update.getProperty(), update.getNewValueBuilder().newValue(model));
					}
				}
	
			}
			transaction.commit();
		}
	
	}

}
