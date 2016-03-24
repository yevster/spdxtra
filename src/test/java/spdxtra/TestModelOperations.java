package spdxtra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ext.com.google.common.base.Joiner;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quackware.spdxtra.DatasetAutoAbortTransaction;
import org.quackware.spdxtra.DatasetInfo;
import org.quackware.spdxtra.ModelOperations;
import org.quackware.spdxtra.NoneNoAssertionOrValue;
import org.quackware.spdxtra.model.SpdxPackageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			DatasetInfo readDataset = ModelOperations.readFromFile(spdxPath);
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
		StringWriter out = new StringWriter();
		try (DatasetAutoAbortTransaction t = DatasetAutoAbortTransaction.begin(readDataset.getDataset(),
				ReadWrite.READ);) {
			model = readDataset.getDataset().getDefaultModel();
			assertNotNull(model);
			assertEquals("http://spdx.org/rdf/terms#", model.getNsPrefixURI(""));

		}
	}

	@Test
	public void testJsonLd() throws IOException{
		DatasetInfo readDataset = getDefaultDataSet();
		String jsonLd = ModelOperations.toJsonLd(readDataset);
		assertTrue(StringUtils.isNotBlank(jsonLd));
		//Weak, but temporary (is it ever?)
		assertTrue(StringUtils.contains(jsonLd, "@graph"));
		assertTrue(StringUtils.contains(jsonLd, "\"rdfs\" : \"http://www.w3.org/2000/01/rdf-schema#\""));
	}
	
	@Test
	public void testJsonRdf() throws IOException{
		DatasetInfo readDataset = getDefaultDataSet();
		String jsonRdf = ModelOperations.toJsonRdf(readDataset);
		assertTrue(StringUtils.isNotBlank(jsonRdf));
	}
	
	@Test
	public void testPackageList() throws IOException{
		DatasetInfo readDataset = getDefaultDataSet();
		Iterable<SpdxPackageInfo> packages = ModelOperations.getAllPackages(readDataset);
		SpdxPackageInfo pkg = Iterables.find(packages, (p)->"SPDXRef-1".equals(p.getSpdxId()));
		

		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getCopyright());
		assertEquals("SPDX tools", pkg.getName());
		assertEquals("2.0.0-RC1", pkg.getVersionInfo().orElse("NO VERSION? AWWWWWW..."));
		
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
