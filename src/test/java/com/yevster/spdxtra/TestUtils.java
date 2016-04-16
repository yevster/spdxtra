package com.yevster.spdxtra;

import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.jena.query.Dataset;
import org.apache.jena.tdb.TDBFactory;

public class TestUtils {

	public static Dataset getDefaultDataSet() {
		try {
			Path spdxPath = Paths.get(
					TestModelOperations.class.getClassLoader().getResource("spdx-tools-2.0.0-RC1.spdx.rdf").toURI());
			Dataset memoryDataset = TDBFactory.createDataset();
			assertTrue(Files.exists(spdxPath));
			Write.rdfIntoDataset(spdxPath, memoryDataset);
			return memoryDataset;
		} catch (URISyntaxException e) {
			throw new RuntimeException("Illegal SPDX input path", e);
		}
	}

	public static <T> String iteratorToString(Iterator<T> iterator) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		while (iterator.hasNext()) {
			if (!first)
				sb.append(", ");
			else
				first = false;
			sb.append(iterator.next().toString());
		}
		return sb.toString();
	}

}
