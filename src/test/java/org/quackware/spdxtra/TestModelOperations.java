package org.quackware.spdxtra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.collect.ImmutableList;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quackware.spdxtra.model.Relationship;
import org.quackware.spdxtra.model.SpdxDocument;
import org.quackware.spdxtra.model.SpdxFile;
import org.quackware.spdxtra.model.SpdxPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestModelOperations {
	private List<Path> tmpToCleanUp;

	private static final Logger logger = LoggerFactory.getLogger(TestModelOperations.class);

	@Before
	public void setup() {
		tmpToCleanUp = new LinkedList<>();
	}

	public static Dataset getDefaultDataSet() {
		try {
			Path spdxPath = Paths.get(TestModelOperations.class.getClassLoader().getResource("spdx-tools-2.0.0-RC1.spdx.rdf").toURI());
			Dataset memoryDataset = TDBFactory.createDataset();
			assertTrue(Files.exists(spdxPath));
			ModelDataAccess.readFromFile(spdxPath, memoryDataset);
			return memoryDataset;
		} catch (URISyntaxException e) {
			throw new RuntimeException("Illegal SPDX input path", e);
		}

	}

	@Test
	public void testNewDataset() throws IOException {
		Dataset dataset = getDefaultDataSet();
		final Model model;
		try (DatasetAutoAbortTransaction t = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ);) {
			model = dataset.getDefaultModel();
			assertNotNull(model);
			assertEquals("http://spdx.org/documents/spdx-toolsv2.0-rc1#", model.getNsPrefixURI(""));

		}
	}

	@Test
	public void testJsonLd() throws IOException {
		Dataset dataset = getDefaultDataSet();
		String jsonLd = ModelDataAccess.toJsonLd(dataset);
		assertTrue(StringUtils.isNotBlank(jsonLd));
		// Weak, but temporary (is it ever?)
		assertTrue(StringUtils.contains(jsonLd, "@graph"));
		assertTrue(StringUtils.contains(jsonLd, "\"rdfs\" : \"http://www.w3.org/2000/01/rdf-schema#\""));
	}

	@Test
	public void testJsonRdf() throws IOException {
		Dataset dataset = getDefaultDataSet();
		String jsonRdf = ModelDataAccess.toJsonRdf(dataset);
		assertTrue(StringUtils.isNotBlank(jsonRdf));
	}

	@Test
	public void testSpdxDocumentInfoAndRelationships() {
		Dataset dataset = getDefaultDataSet();
		SpdxDocument doc = ModelDataAccess.getDocument(dataset);
		assertEquals("SPDX-2.0", doc.getSpecVersion());
		assertEquals("SPDX tools", doc.getName());
		assertEquals("http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-DOCUMENT", doc.getDocumentNamespace());

		Iterable<Relationship> related = ModelDataAccess.getRelationships(dataset, doc);
		Iterator<Relationship> it = related.iterator();

		// There should only be one relationship - describes.
		assertTrue(it.hasNext());
		Relationship describesRelationship = it.next();
		assertTrue(!it.hasNext());
		assertEquals(Relationship.Type.DESCRIBES, describesRelationship.getType());
		assertTrue(describesRelationship.getRelatedElement() instanceof SpdxPackage);
		assertEquals("http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1", describesRelationship.getRelatedElement().getUri());
		assertEquals("[DESCRIBES](SpdxPackage)http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1", describesRelationship.toString());
		// Make sure we don't create a new object each time we call
		// getRelatedElement().
		assertTrue(describesRelationship.getRelatedElement() == describesRelationship.getRelatedElement());

	}
	
	
	@Test
	public void testElementRetrieval(){
		Dataset dataset = getDefaultDataSet();
		Resource docResource = ModelDataAccess.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-DOCUMENT").orElse(null);
		assertNotNull(docResource);
		
		Optional<Resource> shouldBeEmpty = ModelDataAccess.lookupResourceByUri(dataset, "foo://No esta aqui");
		assertEquals(Optional.empty(), shouldBeEmpty);
		
		
	}

	@After
	public void cleanUp() {
		for (Path path : tmpToCleanUp) {
			try {
				FileUtils.forceDelete(path.toFile());
			} catch (Exception e) {
			}
		}
	}
}
