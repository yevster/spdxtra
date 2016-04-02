package com.yevster.spdxtra;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.input.ReaderInputStream;
import com.google.common.collect.Iterators;
import com.yevster.spdxtra.model.Relationship;
import com.yevster.spdxtra.model.SpdxDocument;
import com.yevster.spdxtra.model.SpdxElement;
import com.yevster.spdxtra.model.SpdxFile;
import com.yevster.spdxtra.model.SpdxPackage;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterGraphRIOT;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * @author yevster
 *
 *         Contains all logic for reading from datastore.
 *
 *         Copyright (c) 2016 Yev Bronshteyn. Committed under Apache-2.0 License
 */
public class Read {
	public static class Document{

		/**
		 * Obtains the SPDX Document described in the provided dataset. Per SPDX 2.0
		 * specification, there should be exactly one in a file, so only the first
		 * match will be returned.
		 * 
		 * @param dataset
		 * @return
		 */
		public static SpdxDocument get(Dataset dataset) {
			try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);) {
		
				String sparql = Read.createSparqlQueryByType(SpdxDocument.RDF_TYPE);
				QueryExecution qe = QueryExecutionFactory.create(sparql, dataset);
				ResultSet results = qe.execSelect();
				assert (results.hasNext()); // There should always be one document
											// per SPDX File.
				RDFNode subject = results.next().get("s");
				assert (subject.isResource());
				return new SpdxDocument(subject.asResource());
			}
		}
		
	}
	public static class Package{

		/**
		 * Returns a lazy iteraterable of SpdxFiles.
		 * 
		 * @return
		 */
		public static Iterator<SpdxFile> getFiles(SpdxPackage pkg) {
		
			Resource hasFileResource = pkg.getPropertyAsResource(SpdxUris.SPDX_TERMS + "hasFile");
			final StmtIterator it = hasFileResource.listProperties();
		
			return Iterators.transform(it, (s) -> {
				String uri = s.getSubject().getURI();
				return new SpdxFile(hasFileResource.getModel().getResource(uri));
			});
		}
		
	}

	static final Logger logger = LoggerFactory.getLogger(Read.class);

	private static String createSparqlQueryByType(String typeUri) {
		return "SELECT ?s  WHERE { ?s  <" + SpdxProperties.RDF_TYPE + ">  <" + typeUri + "> }";
	}

	private static String createSparqlQueryBySubjectAndPredicate(String subjectUri, String predicateUri) {
		return "SELECT ?o where { <" + subjectUri + "> <" + predicateUri + "> ?o}";
	}

	public static void outputRdfXml(Dataset dataset, Path outputFilePath) throws IOException {
		Objects.requireNonNull(dataset);
		Objects.requireNonNull(outputFilePath);
		Files.createFile(outputFilePath);
		try (FileOutputStream fos = new FileOutputStream(outputFilePath.toFile());
				DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);) {
			WriterGraphRIOT writer = RDFDataMgr.createGraphWriter(RDFFormat.RDFXML_PRETTY);
			writer.write(fos, dataset.asDatasetGraph().getDefaultGraph(), PrefixMapFactory.create(dataset.getDefaultModel().getNsPrefixMap()), null, dataset.getContext());
		}
	}

	/**
	 * Produces prettified JSON-LD form of SPDX with SPDX terms spirited into
	 * the context and out of the document.
	 * 
	 * @param dataset
	 * @return
	 */
	public static String outputJsonLd(Dataset dataset) {
		// TODO: Remove in-memory limitation
		Object jsonLdRaw = null;
		String jsonLdRawString = null;
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);
				StringWriter out = new StringWriter();) {
			logger.debug("Starting raw JSON-LD output");
			RDFDataMgr.write(out, dataset, Lang.JSONLD);
			out.flush();
			jsonLdRawString = out.toString();
			logger.debug("Raw jsonld produced.");
			jsonLdRaw = JsonUtils.fromInputStream(new ReaderInputStream(new StringReader(jsonLdRawString)));
			logger.debug("Raw JSON parsed.");
		} catch (IOException ioe) {
			if (jsonLdRawString == null)
				throw new RuntimeException("unable to generate JSON", ioe);
			else {
				logger.error("Unable to pretify JSON. The resulting JSON will not look pretty", ioe);
				return jsonLdRawString;
			}
		}
		Object jsonLdContext = null;

		try {
			InputStream jsonContextStream = Read.class.getClassLoader().getResourceAsStream("spdxContext.json");
			jsonLdContext = JsonUtils.fromInputStream(jsonContextStream);
			jsonContextStream.close();
		} catch (IOException e) {
			logger.error("Unable to read JSON context. The resulting JSON will not look pretty", e);
		}
		if (jsonLdContext == null || jsonLdRaw == null) {
			logger.error("Unable to pretify JSON. The resulting JSON will not look pretty");
			return jsonLdRawString;
		}
		JsonLdOptions options = new JsonLdOptions();

		try {
			Object result = JsonLdProcessor.flatten(jsonLdRaw, jsonLdContext, options);
			return JsonUtils.toPrettyString(result);
		} catch (JsonLdError | IOException e) {
			logger.error("Unable to flatten JSON-LD. Raw (ugly) JSON-LD will be returned", e);
			return jsonLdRawString;
		}

	}

	public static Iterator<SpdxPackage> getAllPackages(Dataset dataset) {

		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);) {

			String sparql = createSparqlQueryByType(SpdxUris.SPDX_PACKAGE);
			QueryExecution qe = QueryExecutionFactory.create(sparql, dataset);
			ResultSet results = qe.execSelect();

			return Iterators.transform(results, (QuerySolution qs) -> {
				RDFNode subject = qs.get("s");
				return new SpdxPackage(subject.asResource());
			});
		}
	}

	/**
	 * Returns all the relationships element has and the targets of those
	 * relationships. Does not return the relationships for which
	 * relationshipSource itself is a target.
	 * 
	 * For example, if element is an SpdxDocument that describes a package, the
	 * DESCRIBES relationship from the document to the package will be returned.
	 * However, the DESCRIBED_BY relationship from the package to the document
	 * will not be returned.
	 * 
	 * @param relationshipSource
	 * @return
	 */
	public static Iterator<Relationship> getRelationships(Dataset dataset, SpdxElement element) {
		String sparql = createSparqlQueryBySubjectAndPredicate(element.getUri(), SpdxUris.SPDX_RELATIONSHIP);

		return getRelationshipsWithSparql(dataset, sparql);
	}

	private static Iterator<Relationship> getRelationshipsWithSparql(Dataset dataset, String sparql) {
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);) {
			QueryExecution qe = QueryExecutionFactory.create(sparql, dataset);
			ResultSet results = qe.execSelect();
			return Iterators.transform(results, (QuerySolution qs) -> {
				RDFNode relationshipNode = qs.get("o");
				assert (relationshipNode.isResource());
				return new Relationship(relationshipNode.asResource());
			});

		}
	}

	public static Iterator<Relationship> getRelationships(Dataset dataset, SpdxElement element, Relationship.Type relationshipType) {
		String sparql = "SELECT ?o  WHERE { <" + element.getUri() + "> <" + SpdxUris.SPDX_RELATIONSHIP + "> ?o.\n" + "?o <"
				+ SpdxUris.SPDX_TERMS + "relationshipType" + "> <" + relationshipType.getUri() + ">. }";
		return getRelationshipsWithSparql(dataset, sparql);
	}

	public static Optional<Resource> lookupResourceByUri(Dataset dataset, String uri) {
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);) {
			Model model = dataset.getDefaultModel();
			// Although this would create a new resource if it didn't exist,
			// we're in a read-only transaction.
			Resource result = model.createResource(uri);
			if (!result.listProperties().hasNext()) { // an existing resource
														// would have at least a
														// type
				return Optional.empty();
			} else
				return Optional.of(result);
		}
	}

}
