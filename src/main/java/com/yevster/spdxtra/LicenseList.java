package com.yevster.spdxtra;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.yevster.spdxtra.model.write.License;
import com.yevster.spdxtra.util.MiscUtils;

public enum LicenseList {
	INSTANCE;

	public static class ListedLicense extends License {
		private String name;
		private String id;
		private boolean osiApproved;

		/*
		 * The resources these licenses are built from have a ton of useless
		 * HTML content and are kept in memory. So rather than hold them
		 * indefinitely, as with other RDF-based model objects, we'll just copy
		 * over the good parts and let them go out of scope.
		 */
		protected ListedLicense(Resource r) {
			this.id = r.getProperty(SpdxProperties.LICENSE_ID).getString();
			this.name = r.getProperty(SpdxProperties.NAME).getString();
			this.osiApproved = r.getProperty(SpdxProperties.OSI_APPROVED).getBoolean();

		}

		/**
		 * Returns the friendly name of the license.
		 * 
		 * @return
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the short license ID as defined in the license list.
		 * 
		 * @return
		 */
		public String getLicenseId() {
			return id;
		}

		/**
		 * Returns true if, and only if, this license is OSI approved.
		 */
		public boolean isOsiApproved() {
			return osiApproved;
		}

		public RDFNode getRdfNode(Model m) {
			return ResourceFactory.createResource(SpdxUris.LISTED_LICENSE_NAMESPACE + id);
		}

		/**
		 * Saves all the information available for the license, beyond what is
		 * used in SPDX. Can be used to clone or perist the license list.
		 */
		public RDFNode getRdfNodeFull(Model m) {
			String uri = getRdfNode(null).asResource().getURI();
			Resource resource = m.createResource(uri);
			resource.addProperty(SpdxProperties.LICENSE_ID, getLicenseId());
			resource.addLiteral(SpdxProperties.OSI_APPROVED, isOsiApproved());
			resource.addProperty(SpdxProperties.NAME, getName());
			return resource;
		}
	}

	private final Map<String, ListedLicense> retrievedListedLicenses;

	private final String version;

	private LicenseList() {
		String licenseListLocation = System.getProperty(Constants.LICENSE_LIST_LOCATION_PROPERTY);
		try {
			Path licenseListPath = StringUtils.isNotBlank(licenseListLocation) ? Paths.get(licenseListLocation)
					: Paths.get(this.getClass().getClassLoader().getResource("licenseList.bin").toURI());
			Dataset dataset = DatasetFactory.create();
			try (InputStream is = Files.newInputStream(licenseListPath)) {
				RDFDataMgr.read(dataset, is, Lang.RDFTHRIFT);
			}

			Resource mainResource = dataset.getDefaultModel().getResource(Constants.LICENSE_LIST_URL);
			version = mainResource.getProperty(SpdxProperties.LICENSE_LIST_VERSION).getString();
			retrievedListedLicenses = MiscUtils.toLinearStream(mainResource.listProperties(SpdxProperties.LICENSE_LIST_LICENSE))
					.map(Statement::getObject)
					.map(RDFNode::asResource)
					.map(ListedLicense::new)
					.collect(Collectors.toMap(ListedLicense::getLicenseId, Function.identity()));

		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException("Unable to initialize license list");
		}
	}

	/**
	 * Returns the listed license with the suggested ID or Optional.empty() if a
	 * license with such ID does not exist.
	 * 
	 * @param id
	 * @return
	 */
	public Optional<ListedLicense> getListedLicenseById(String id) {
		return Optional.ofNullable(retrievedListedLicenses.get(id));

	}

	/**
	 * Returns the license list version.
	 * 
	 * @return
	 */
	public String getVersion() {
		return version;
	}

}
