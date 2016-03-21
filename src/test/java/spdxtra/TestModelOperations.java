package spdxtra;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;
import org.quackware.spdxtra.DatasetAutoAbortTransaction;
import org.quackware.spdxtra.DatasetInfo;
import org.quackware.spdxtra.ModelOperations;

public class TestModelOperations {
	@Test
	public void testNewDataset() throws URISyntaxException{
		Path spdxPath = Paths.get(getClass().getClassLoader().getResource("SPDXParser.spdx").toURI());
		assertNotNull(spdxPath);
		assertTrue(Files.exists(spdxPath));
		DatasetInfo readDataset = ModelOperations.readFromFile(spdxPath);
		assertNotNull(readDataset);
		assertTrue(Files.isDirectory(readDataset.getDatasetPath()));
		
		final Model model;
		try(DatasetAutoAbortTransaction t = DatasetAutoAbortTransaction.begin(readDataset.getDataset(), ReadWrite.READ);){
			model = readDataset.getDataset().getDefaultModel();
			assertNotNull(model);
			assertEquals("http://spdx.org/rdf/terms#", model.getNsPrefixURI(""));
		}
	}
}
