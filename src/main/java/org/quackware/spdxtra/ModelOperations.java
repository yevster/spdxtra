package org.quackware.spdxtra;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

/**
 * @author yevster
 *
 *         Static model manipulation logic
 *
 *         Copyright (c) 2016 Yev Bronshteyn. Committed under Apache-2.0 License
 */
public class ModelOperations {

	private static final Logger logger = LoggerFactory.getLogger(ModelOperations.class);

	/**
	 * Reads inputFilePath and popualtes a new RDF data store at
	 * targetDirectoryPath with its contents.
	 * 
	 * @param inputFilePath
	 *            Must be a valid path to an RDF file.
	 * @return
	 */
	public static DatasetInfo readFromFile(Path inputFilePath) {
		Objects.requireNonNull(inputFilePath);
		if (Files.notExists(inputFilePath) && Files.isRegularFile(inputFilePath))
			throw new IllegalArgumentException("File " + inputFilePath.toAbsolutePath().toString() + " does not exist");
		final Path datasetPath;
		try {
			datasetPath = Files.createTempDirectory(inputFilePath.getFileName().toString());
			logger.debug("Creating new TDB in " + datasetPath.toAbsolutePath().toString());
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to create temp directory", ioe);
		}
		final InputStream is;
		try {
			is = Files.newInputStream(inputFilePath);
		} catch (IOException ioe) {
			throw new RuntimeException("Unable to read file " + inputFilePath.toAbsolutePath().toString(), ioe);
		}
		Dataset dataset = TDBFactory.createDataset(datasetPath.toAbsolutePath().toString());
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.WRITE);) {
			dataset.getDefaultModel().read(is, null);
			transaction.commit();
		}
		return new DatasetInfo(dataset, datasetPath);
	}

	
	/**
	 * Produces prettified JSON-LD form of SPDX with SPDX terms spirited into the context and out of the document.
	 * @param datasetInfo
	 * @return
	 */
	public static String toJsonLd(DatasetInfo datasetInfo) {
		// TODO: Remove in-memory limitation
		Object jsonLdRaw = null;
		String jsonLdRawString = null;
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(datasetInfo.getDataset(),
				ReadWrite.READ);
				StringWriter out = new StringWriter();){
			logger.debug("Starting raw JSON-LD output");
			RDFDataMgr.write(out, datasetInfo.getDataset(), Lang.JSONLD);
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
			InputStream jsonContextStream = ModelOperations.class.getClassLoader()
					.getResourceAsStream("spdxContext.json");
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
	
	public static String toJsonRdf(DatasetInfo datasetInfo){
		String jsonRdfString = null;
		try (DatasetAutoAbortTransaction transaction = DatasetAutoAbortTransaction.begin(datasetInfo.getDataset(),
				ReadWrite.READ);
				StringWriter out = new StringWriter();){
			logger.debug("Starting raw JSON/RDF output");
			RDFDataMgr.write(out, datasetInfo.getDataset().getDefaultModel(), RDFFormat.RDFJSON);
			out.flush();
			jsonRdfString = out.toString();
		} catch (IOException ioe){
			throw new RuntimeException("Error generating JSON/RDF", ioe);
		}
		return jsonRdfString;
	}
}
