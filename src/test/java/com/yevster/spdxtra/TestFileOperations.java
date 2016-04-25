package com.yevster.spdxtra;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.tdb.TDB;
import org.junit.Before;
import org.junit.Test;

import com.yevster.spdxtra.model.Creator;
import com.yevster.spdxtra.model.SpdxDocument;
import com.yevster.spdxtra.model.SpdxFile;
import com.yevster.spdxtra.model.SpdxPackage;
import static org.junit.Assert.*;

public class TestFileOperations {
	private static final String baseUrl = "http://www.example.org/baseUrl";
	private static final String packageSpdxId = "SPDXRef-PKG";
	private static final String packageUri = baseUrl + "#" + packageSpdxId;

	private SpdxPackage pkg;
	private Dataset dataset;

	@Before
	public void setUp() {
		dataset = DatasetFactory.createTxnMem();
		final String docSpdxId = "SPDXRef-Document";
		Write.applyUpdatesInOneTransaction(dataset,
				Write.New.document(baseUrl, docSpdxId, "My Document", Creator.person("Mimi Me", Optional.empty())),
				Write.Document.addDescribedPackage(baseUrl, docSpdxId, packageSpdxId, "The packagy package"));
		reloadPackage();
	}

	public void reloadPackage() {
		pkg = new SpdxPackage(Read.lookupResourceByUri(dataset, packageUri).get());
	}

	@Test
	public void testAddFileToPackage() {
		assertEquals("Newly-created package should have no files", 0, pkg.getFiles().count());
		final String fileName="./foo/bar/whatevs.txt";

		Write.applyUpdatesInOneTransaction(dataset,
				Write.Package.addFile(baseUrl, packageSpdxId, "myFile", fileName));
		reloadPackage();
		List<SpdxFile> allFilesInPackage = pkg.getFiles().collect(Collectors.toList());
		assertEquals("Expected to one file in package after adding one file.", 1, allFilesInPackage.size());
		SpdxFile file = allFilesInPackage.get(0);
		assertEquals("http://www.example.org/baseUrl#myFile", file.getUri());
		assertEquals(fileName, file.getFileName());
		
				

	}

}
