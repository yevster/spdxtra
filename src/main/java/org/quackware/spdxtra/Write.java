package org.quackware.spdxtra;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.quackware.spdxtra.model.SpdxPackage;

public final class Write {
	public static final class New{
		
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
