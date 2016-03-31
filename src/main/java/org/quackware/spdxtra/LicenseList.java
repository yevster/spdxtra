package org.quackware.spdxtra;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.quackware.spdxtra.model.License;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import net.rootdev.javardfa.ParserFactory;
import net.rootdev.javardfa.ParserFactory.Format;
import net.rootdev.javardfa.StatementSink;
import net.rootdev.javardfa.jena.JenaStatementSink;
import net.rootdev.javardfa.jena.RDFaReader.HTMLRDFaReader;

public enum LicenseList {
	INSTANCE;

	public static class ListedLicense extends License {
		private static final Property osiApprovedProperty = new PropertyImpl(SpdxUris.SPDX_TERMS, "isOsiApproved");
		private final RDFNode rdfNode;
		private String name;
		private String id;
		private boolean osiApproved;

		/*
		 * The resources these licenses are built from have a ton of useless
		 * HTML content and are kept in memory. So rather than hold them
		 * indefinitely, as with other RDF-based model objects, we'll just copy
		 * over the good parts and let them go out of scope.
		 */
		ListedLicense(Resource r) {
			this.id = r.getProperty(licenseIdProperty).getString();
			this.name = r.getProperty(licenseNameProperty).getString();
			this.osiApproved = r.getProperty(osiApprovedProperty).getBoolean();
			this.rdfNode = ResourceFactory.createResource(SpdxUris.LISTED_LICENSE_NAMESPACE + id);
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

		public RDFNode getRdfNode() {
			return rdfNode;
		}
	}

	public static class LicenseRetrievalException extends RuntimeException {
		public LicenseRetrievalException(String s, Throwable cause) {
			super(s, cause);
		}

		public LicenseRetrievalException(String s) {
			super(s);
		}
	}

	private HashMap<String, ListedLicense> retrievedListedLicenses = new HashMap<String, ListedLicense>();

	private LicenseList() {

	}

	/**
	 * Returns the listed license with the suggested ID or Optional.empty() if a
	 * license with such ID does not exist.
	 * 
	 * @param id
	 * @return
	 */
	public Optional<ListedLicense> getListedLicenseById(String id) {
		// Verify arguments
		if (StringUtils.isBlank(id)) {
			throw new IllegalArgumentException("Cannot get listed license with null or empty id");
		} // For security
		if (StringUtils.containsAny(id, '/', ':')) {
			throw new IllegalArgumentException("Illegal characters in id " + id);
		}

		// Have we already retrieved?
		if (retrievedListedLicenses.containsKey(id))
			return Optional.of(retrievedListedLicenses.get(id));

		String licenseUri = SpdxUris.LISTED_LICENSE_NAMESPACE + id;

		try {
			Model model = ModelFactory.createDefaultModel();
			HttpClient httpClient = new DefaultHttpClient();
			HttpUriRequest request = new HttpGet(licenseUri);
			HttpResponse response = httpClient.execute(request);

			if (response.getStatusLine().getStatusCode() == 404) {
				return Optional.empty();
			}
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new LicenseRetrievalException("Error accessing " + licenseUri + ". Status returned: "
						+ response.getStatusLine().getStatusCode() + ". Reason:"
						+ response.getStatusLine().getReasonPhrase());
			}

			new HTMLRDFaReader().read(model, response.getEntity().getContent(), "http://www.w3.org/1999/xhtml:html");

			Resource foundLicense = model.listSubjects().next();
			ListedLicense result = new ListedLicense(foundLicense);
			retrievedListedLicenses.put(id, result);
			return Optional.of(result);
		} catch (IOException e) {
			throw new LicenseRetrievalException("Error accessing " + licenseUri, e);
		}

	}

	private static final void populateModelFromRdfa(String content, Model targetModel) {
		StatementSink sink = new JenaStatementSink(targetModel);
		try {
			XMLReader parser = ParserFactory.createReaderForFormat(sink, Format.HTML);
			parser.parse(new InputSource(content));
		} catch (SAXException | IOException se) {
			throw new LicenseList.LicenseRetrievalException("Unable to parse retrieved license", se);
		}

	}

}
