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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quackware.spdxtra.model.Relationship;
import org.quackware.spdxtra.model.SpdxDocument;
import org.quackware.spdxtra.model.SpdxFile;
import org.quackware.spdxtra.model.SpdxPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.org.apache.xpath.internal.operations.Mod;

public class TestModelOperations {
	private List<Path> tmpToCleanUp;

	private static final Logger logger = LoggerFactory.getLogger(TestModelOperations.class);


	@Before
	public void setup() {
		tmpToCleanUp = new LinkedList<>();
	}

	private DatasetInfo getDefaultDataSet() {
		try {

			Path spdxPath = Paths.get(getClass().getClassLoader().getResource("spdx-tools-2.0.0-RC1.spdx.rdf").toURI());
			assertNotNull(spdxPath);
			assertTrue(Files.exists(spdxPath));
			DatasetInfo readDataset = ModelDataAccess.readFromFile(spdxPath);
			tmpToCleanUp.add(readDataset.getDatasetPath());
			assertNotNull(readDataset);
			assertTrue(Files.isDirectory(readDataset.getDatasetPath()));

			return readDataset;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testNewDataset() throws IOException {
		DatasetInfo readDataset = getDefaultDataSet();
		final Model model;
		try (DatasetAutoAbortTransaction t = DatasetAutoAbortTransaction.begin(readDataset.getDataset(),
				ReadWrite.READ);) {
			model = readDataset.getDataset().getDefaultModel();
			assertNotNull(model);
			assertEquals("http://spdx.org/documents/spdx-toolsv2.0-rc1#", model.getNsPrefixURI(""));

		}
	}

	@Test
	public void testJsonLd() throws IOException{
		DatasetInfo readDataset = getDefaultDataSet();
		String jsonLd = ModelDataAccess.toJsonLd(readDataset);
		assertTrue(StringUtils.isNotBlank(jsonLd));
		//Weak, but temporary (is it ever?)
		assertTrue(StringUtils.contains(jsonLd, "@graph"));
		assertTrue(StringUtils.contains(jsonLd, "\"rdfs\" : \"http://www.w3.org/2000/01/rdf-schema#\""));
	}
	
	@Test
	public void testJsonRdf() throws IOException{
		DatasetInfo readDataset = getDefaultDataSet();
		String jsonRdf = ModelDataAccess.toJsonRdf(readDataset);
		assertTrue(StringUtils.isNotBlank(jsonRdf));
	}
	
	@Test
	public void testPackageRead() throws IOException{
		DatasetInfo readDataset = getDefaultDataSet();
		Iterable<SpdxPackage> packages = ModelDataAccess.getAllPackages(readDataset);
		SpdxPackage pkg = Iterables.find(packages, (p)->"SPDXRef-1".equals(p.getSpdxId()));

		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getCopyright());
		assertEquals("SPDX tools", pkg.getName());
		assertEquals("2.0.0-RC1", pkg.getVersionInfo().orElse("NO VERSION? AWWWWWW..."));
		assertEquals(12, Iterables.size(ModelDataAccess.getFilesForPackage(pkg)));
		

	}
	
	@Test
	public void testSpdxDocumentInfoAndRelationships() throws IOException{
		DatasetInfo readDataset = getDefaultDataSet();
		SpdxDocument doc = ModelDataAccess.getDocument(readDataset);
		assertEquals("SPDX-2.0", doc.getSpecVersion());
		assertEquals("SPDX tools", doc.getName());
		assertEquals("http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-DOCUMENT", doc.getDocumentNamespace());
		

		Iterable<Relationship> related = ModelDataAccess.getRelationships(readDataset, doc);
		Iterator<Relationship> it = related.iterator();
		
		//There should only be one relationship - describes.
		assertTrue(it.hasNext());
		Relationship describesRelationship = it.next();
		assertTrue(!it.hasNext());
		assertEquals(Relationship.Type.DESCRIBES, describesRelationship.getType());
		assertTrue(describesRelationship.getRelatedElement() instanceof SpdxPackage);
		assertEquals("http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1", describesRelationship.getRelatedElement().getUri());
		assertEquals("[DESCRIBES](SpdxPackage)http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1", describesRelationship.toString());
		//Make sure we don't create a new object each time we call getRelatedElement().
		assertTrue(describesRelationship.getRelatedElement() == describesRelationship.getRelatedElement());
	
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
