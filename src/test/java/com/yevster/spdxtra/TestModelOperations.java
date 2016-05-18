package com.yevster.spdxtra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yevster.spdxtra.DatasetAutoAbortTransaction;
import com.yevster.spdxtra.Read;
import com.yevster.spdxtra.Read.Document;
import com.yevster.spdxtra.model.Relationship;
import com.yevster.spdxtra.model.SpdxDocument;
import com.yevster.spdxtra.model.SpdxPackage;

public class TestModelOperations {
	private List<Path> tmpToCleanUp;

	private static final Logger logger = LoggerFactory.getLogger(TestModelOperations.class);

	@Before
	public void setup() {
		tmpToCleanUp = new LinkedList<>();
	}

	@Test
	public void testNewDataset() throws IOException {
		Dataset dataset = TestUtils.getDefaultDataSet();
		final Model model;
		try (DatasetAutoAbortTransaction t = DatasetAutoAbortTransaction.begin(dataset, ReadWrite.READ)) {
			model = dataset.getDefaultModel();
			assertNotNull(model);
			assertEquals("http://spdx.org/documents/spdx-toolsv2.0-rc1#", model.getNsPrefixURI(""));

		}
	}

	@Test
	public void testJsonLd() throws IOException {
		Dataset dataset = TestUtils.getDefaultDataSet();
		String jsonLd = Read.outputJsonLd(dataset);
		assertTrue(StringUtils.isNotBlank(jsonLd));
		// Weak, but temporary (is it ever?)
		assertTrue(StringUtils.contains(jsonLd, "@graph"));
		assertTrue(StringUtils.contains(jsonLd, "\"rdfs\" : \"http://www.w3.org/2000/01/rdf-schema#\""));
	}

	@Test
	public void testSpdxDocumentInfoAndRelationships() {
		Dataset dataset = TestUtils.getDefaultDataSet();
		SpdxDocument doc = Document.get(dataset);
		assertEquals("SPDX-2.0", doc.getSpecVersion());
		assertEquals("SPDX tools", doc.getName());
		assertEquals("http://spdx.org/documents/spdx-toolsv2.0-rc1", doc.getDocumentNamespace());

		Iterator<Relationship> related = Read.getRelationships(dataset, doc).iterator();

		// There should only be one relationship - describes.
		assertTrue(related.hasNext());
		Relationship describesRelationship = related.next();
		assertTrue(!related.hasNext());
		assertEquals(Relationship.Type.DESCRIBES, describesRelationship.getType());
		assertTrue(describesRelationship.getRelatedElement() instanceof SpdxPackage);
		assertEquals("http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1",
				describesRelationship.getRelatedElement().getUri());
		assertEquals("[DESCRIBES](SpdxPackage)http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1",
				describesRelationship.toString());

		// Verify the date
		ZonedDateTime creationTime = doc.getCreationInfo().getCreationDate();
		ZonedDateTime expectedCreationTime = ZonedDateTime.of(LocalDateTime.of(2015, 8, 3, 21, 38, 16),
				ZoneId.of("UTC"));
		assertEquals(expectedCreationTime, creationTime);
		assertEquals(Optional.of("Created for Linux Con. SPDX Bakeoff 2015"), doc.getCreationInfo().getComment());

		// Make sure we don't create a new object each time we call
		// getRelatedElement().
		assertTrue(describesRelationship.getRelatedElement() == describesRelationship.getRelatedElement());

	}

	@Test
	public void testElementRetrieval() {
		Dataset dataset = TestUtils.getDefaultDataSet();
		Resource docResource = Read
				.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-DOCUMENT")
				.orElse(null);
		assertNotNull(docResource);

		Optional<Resource> shouldBeEmpty = Read.lookupResourceByUri(dataset, "foo://No esta aqui");
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
