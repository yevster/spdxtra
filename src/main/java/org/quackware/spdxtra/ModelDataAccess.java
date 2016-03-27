package org.quackware.spdxtra;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.tdb.TDBFactory;
import org.quackware.spdxtra.model.Relationship;
import org.quackware.spdxtra.model.SpdxDocument;
import org.quackware.spdxtra.model.SpdxElement;
import org.quackware.spdxtra.model.SpdxFile;
import org.quackware.spdxtra.model.SpdxPackage;
import org.quackware.spdxtra.util.MiscUtils;
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
public class ModelDataAccess {

	public class IllegalUpdateException extends RuntimeException {
		public IllegalUpdateException(String s) {
			super(s);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(ModelDataAccess.class);

	private static String createSparqlQueryByType(String typeUri) {
		return "SELECT ?s  WHERE { ?s  <" + RdfResourceRepresentation.RDF_TYPE + ">  <" + typeUri + "> }";
	}

	private static String createSparqlQueryBySubjectAndPredicate(String subjectUri, String predicateUri) {
		return "SELECT ?o where { <" + subjectUri + "> <" + predicateUri + "> ?o}";
	}

	/**
	 * Reads inputFilePath and popualtes a new RDF data store at
	 * targetDirectoryPath with its contents.
	 * 
	 * @param inputFilePath
	 *            Must be a valid path to an RDF file.
	 * @param newDatasetPath
	 *            The path to which to persist the RDF triple store with SPDX
	 *            data.
	 * @return
	 */
	public static Dataset readFromFile(Path inputFilePath, Path newDatasetPath) {
		Objects.requireNonNull(newDatasetPath);

		if (Files.notExists(newDatasetPath) || !Files.isDirectory(newDatasetPath)) {
			throw new IllegalArgumentException("Invalid dataset path: " + newDatasetPath.toAbsolutePath().toString());
		}
		logger.debug("Creating new TDB in " + newDatasetPath.toAbsolutePath().toString());

		Dataset dataset = TDBFactory.createDataset(newDatasetPath.toString());
		readFromFile(inputFilePath, dataset);
		return dataset;
	}

	/**
	 * Reads RDF from inputFilePath into a provided dataset. NOTE: This behavior
	 * is not tested with pre-populated datasets.
	 * 
	 * @param inputFilePath
	 * @param dataset
	 */
	public static void readFromFile(Path inputFilePath, Dataset dataset) {
		Objects.requireNonNull(inputFilePath);
		if (Files.notExists(inputFilePath) && Files.isRegularFile(inputFilePath))
			throw new IllegalArgumentException("File " + inputFilePath.toAbsolutePath().toString() + " does not exist");

		final InputStream is;
		try {
			is = Files.newInputStream(inputFilePath);
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to read file " + inputFilePath.toAbsolutePath().toString(), ioe);
		}

		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.WRITE);) {
			dataset.getDefaultModel().read(is, null);
			transaction.commit();
		}

	}

	/**
	 * Produces prettified JSON-LD form of SPDX with SPDX terms spirited into
	 * the context and out of the document.
	 * 
	 * @param dataset
	 * @return
	 */
	public static String toJsonLd(Dataset dataset) {
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
			InputStream jsonContextStream = ModelDataAccess.class.getClassLoader().getResourceAsStream("spdxContext.json");
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

	public static String toJsonRdf(Dataset dataset) {
		String jsonRdfString = null;
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);
				StringWriter out = new StringWriter();) {
			logger.debug("Starting raw JSON/RDF output");
			RDFDataMgr.write(out, dataset.getDefaultModel(), RDFFormat.RDFJSON);
			out.flush();
			jsonRdfString = out.toString();
		} catch (IOException ioe) {
			throw new RuntimeException("Error generating JSON/RDF", ioe);
		}
		return jsonRdfString;
	}

	public static Iterable<SpdxPackage> getAllPackages(Dataset dataset) {

		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);) {

			String sparql = createSparqlQueryByType(SpdxPackage.RDF_TYPE);
			QueryExecution qe = QueryExecutionFactory.create(sparql, dataset);
			ResultSet results = qe.execSelect();

			return MiscUtils.fromIteratorConsumer(results, (QuerySolution qs) -> {
				RDFNode subject = qs.get("s");
				return new SpdxPackage(subject.asResource());
			});
		}
	}

	/**
	 * Obtains the SPDX Document described in the provided model. Per SPDX 2.0
	 * specification, there should be exactly one in a file, so only the first
	 * match will be returned.
	 * 
	 * @param dataset
	 * @return
	 */
	public static SpdxDocument getDocument(Dataset dataset) {
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);) {

			String sparql = createSparqlQueryByType(SpdxDocument.RDF_TYPE);
			QueryExecution qe = QueryExecutionFactory.create(sparql, dataset);
			ResultSet results = qe.execSelect();
			assert (results.hasNext()); // There should always be one document
										// per SPDX File.
			RDFNode subject = results.next().get("s");
			assert (subject.isResource());
			return new SpdxDocument(subject.asResource());
		}
	}

	/**
	 * Returns a lazy iteraterable of SpdxFiles.
	 * 
	 * @return
	 */
	public static Iterable<SpdxFile> getFilesForPackage(SpdxPackage pkg) {

		Resource hasFileResource = pkg.getPropertyAsResource(SpdxUris.SPDX_TERMS + "hasFile");
		final StmtIterator it = hasFileResource.listProperties();

		return MiscUtils.fromIteratorConsumer(it, (s) -> {
			String uri = s.getSubject().getURI();
			return new SpdxFile(hasFileResource.getModel().getResource(uri));
		});
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
	public static Iterable<Relationship> getRelationships(Dataset dataset, SpdxElement element) {
		String sparql = createSparqlQueryBySubjectAndPredicate(element.getUri(), SpdxUris.SPDX_TERMS + "relationship");

		return getRelationshipsWithSparql(dataset, sparql);
	}

	private static Iterable<Relationship> getRelationshipsWithSparql(Dataset dataset, String sparql) {
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);) {
			QueryExecution qe = QueryExecutionFactory.create(sparql, dataset);
			ResultSet results = qe.execSelect();
			return MiscUtils.fromIteratorConsumer(results, (QuerySolution qs) -> {
				RDFNode relationshipNode = qs.get("o");
				assert (relationshipNode.isResource());
				return new Relationship(relationshipNode.asResource());
			});

		}
	}

	public static Iterable<Relationship> getRelationships(Dataset dataset, SpdxElement element, Relationship.Type relationshipType) {
		String sparql = "SELECT ?o  WHERE { <"+element.getUri()+"> <" +  SpdxUris.SPDX_TERMS + "relationship> ?o.\n" 
				+ "?o <" + SpdxUris.SPDX_TERMS + "relationshipType" + "> <" + relationshipType.getUri() + ">. }";
		return getRelationshipsWithSparql(dataset, sparql);
	}

	public static void applyUpdatesInOneTransaction(Dataset dataset, Iterable<RdfResourceUpdate> updates) {
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.WRITE);) {
			Model model = dataset.getDefaultModel();
			for (RdfResourceUpdate update : updates) {
				Resource resource = model.getResource(update.getResourceUri());
				Statement s = resource.getProperty(update.getProperty());
				if (s != null) {
					s.changeObject(update.getNewValueBuilder().newValue(model));
				} else {
					resource.addProperty(update.getProperty(), update.getNewValueBuilder().newValue(model));
				}

			}
			transaction.commit();
		}

	}

}
