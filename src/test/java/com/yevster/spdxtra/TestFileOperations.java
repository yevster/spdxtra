package com.yevster.spdxtra;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.yevster.spdxtra.NoneNoAssertionOrValue.AbsentValue;
import com.yevster.spdxtra.model.Checksum;
import com.yevster.spdxtra.model.Creator;
import com.yevster.spdxtra.model.FileType;
import com.yevster.spdxtra.model.SpdxFile;
import com.yevster.spdxtra.model.SpdxPackage;
import com.yevster.spdxtra.model.write.License;
import com.yevster.spdxtra.util.MiscUtils;

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
		final String fileName = "./foo/bar/whatevs.txt";
		final String expectedFileUrl = baseUrl + "#SPDXRef-myFile";
		final String mockSha1 = "abc135875aeffffff";
		final String mockMd5 = DigestUtils.md5Hex("My sum");
		final String mockSha256 = DigestUtils.sha256Hex("My sum");
		final String extractedLicense = "This is the extracting license. Once this was in a file. Now it is here.";
		final String copyrightText = "Copyright (C) 2016 Aweomsness Awesome, Inc.";
		final String comment = "No comment. Carry on. Wait, this is a comment. Curses!";
		final String artifactOfHomepage = "http://example.org/epic/fail";
		final String artifactOfName = "Epic Failure 1,9";
		final String licenseName = "El nombre";

		Write.applyUpdatesInOneTransaction(dataset, Write.Package.addFile(baseUrl, packageSpdxId, "SPDXRef-myFile", fileName),
				Write.File.fileTypes(expectedFileUrl, FileType.OTHER, FileType.APPLICATION),
				Write.File.concludedLicense(expectedFileUrl, License.NONE),
				Write.File.checksums(expectedFileUrl, mockSha1, Checksum.md5(mockMd5)),
				Write.File.addLicenseInfoInFile(expectedFileUrl,
						License.extracted(extractedLicense, licenseName, baseUrl, "LicenseRef-el")),
				Write.File.artifactOf(expectedFileUrl, artifactOfName, null));
		reloadPackage();
		List<SpdxFile> allFilesInPackage = pkg.getFiles().collect(Collectors.toList());
		assertEquals("Expected to one file in package after adding one file.", 1, allFilesInPackage.size());
		SpdxFile file = allFilesInPackage.get(0);

		assertEquals(expectedFileUrl, file.getUri());
		assertEquals(fileName, file.getFileName());
		assertEquals(Sets.newHashSet(FileType.OTHER, FileType.APPLICATION), file.getFileTypes());
		assertEquals(AbsentValue.NONE.getUri(), file.getPropertyAsResource(SpdxProperties.LICENSE_CONCLUDED).get().getURI());
		assertEquals(Sets.newHashSet(Checksum.md5(mockMd5), Checksum.sha1(mockSha1)), file.getChecksums());
		// Test default copyright - NOASSERTION
		assertEquals(SpdxUris.NO_ASSERTION, file.getCopyrightText().getLiteralOrUriValue());
		assertEquals("Empty optional not returned for uninitialized comment", Optional.empty(), file.getComment());
		assertEquals(Optional.empty(), file.getOptionalPropertyAsString(SpdxProperties.LICENSE_COMMENTS));
		assertEquals(Optional.empty(), file.getNoticeText());
		assertEquals(new HashSet<String>(), file.getContributors());

		Resource doapProject = file.rdfResource.getProperty(SpdxProperties.ARTIFACT_OF).getObject().asResource();
		// The spec supports a value of UNKNOWN, but as other tools validate
		// this field as a URL and it is optional
		// SpdXtra should omit it entirely if blank.
		Assert.assertEquals("When DOAP homepage not specified, should omit.", false,
				doapProject.listProperties(SpdxProperties.DOAP_HOMEPAGE).hasNext());
		Assert.assertEquals(artifactOfName, doapProject.getProperty(SpdxProperties.DOAP_NAME).getString());

		// Test overwrites
		Write.applyUpdatesInOneTransaction(dataset, Write.File.checksums(file.getUri(), mockSha1, Checksum.sha256(mockSha256)),
				Write.File.copyrightText(expectedFileUrl, NoneNoAssertionOrValue.of(copyrightText)),
				Write.File.comment(file.getUri(), comment), Write.File.artifactOf(expectedFileUrl, artifactOfName, artifactOfHomepage),
				Write.File.licenseComments(expectedFileUrl, "Nice file license!"), Write.File.noticeText(file.getUri(), "NOTICE THIS!"),
				Write.File.contributors(file.getUri(), "Larry", "Curly", "Moe"));

		// Reload
		Resource fileResource = Read.lookupResourceByUri(dataset, expectedFileUrl).get();
		file = new SpdxFile(fileResource);
		assertEquals(Sets.newHashSet(Checksum.sha256(mockSha256), Checksum.sha1(mockSha1)), file.getChecksums());
		assertEquals(baseUrl + "#LicenseRef-el", file.rdfResource.getPropertyResourceValue(SpdxProperties.LICENSE_INFO_IN_FILE).getURI());
		assertEquals(copyrightText, file.getCopyrightText().getValue().get());
		assertEquals(comment, file.getComment().get());
		assertEquals(Optional.of("Nice file license!"), file.getOptionalPropertyAsString(SpdxProperties.LICENSE_COMMENTS));
		assertEquals(Optional.of("NOTICE THIS!"), file.getNoticeText());
		assertEquals(Sets.newHashSet("Larry", "Curly", "Moe"), file.getContributors());

		List<Resource> doapProjects = MiscUtils.toLinearStream(fileResource.listProperties(SpdxProperties.ARTIFACT_OF))
				.map(Statement::getObject).map(RDFNode::asResource).collect(Collectors.toList());
		Assert.assertEquals("Applying artifactOf update twice should produce two properties.", 2, doapProjects.size());
		Set<String> expectedHomepages = Sets.newHashSet(null, artifactOfHomepage);

		for (Resource dp : doapProjects) {
			Assert.assertNotNull(dp);
			Assert.assertEquals(artifactOfName, dp.getProperty(SpdxProperties.DOAP_NAME).getString());
			// Use a null value to represent the project with no homepage (which
			// in truth just won't have that property).
			String actualHomepage = dp.getProperty(SpdxProperties.DOAP_HOMEPAGE) != null
					? dp.getProperty(SpdxProperties.DOAP_HOMEPAGE).getString() : null;
			Assert.assertTrue("Homepage has an unexpected value: " + actualHomepage, expectedHomepages.remove(actualHomepage));
			Assert.assertEquals("Unexpected type for DOAP Project resource", "http://usefulinc.com/ns/doap#Project",
					dp.getProperty(SpdxProperties.RDF_TYPE).getObject().asResource().getURI());
		}

	}

	@Test
	public void testFileType() {
		assertEquals("http://spdx.org/rdf/terms#fileType_documentation", FileType.DOCUMENTATION.getUri());
		assertEquals("http://spdx.org/rdf/terms#fileType_other", FileType.OTHER.getUri());
		assertEquals(FileType.OTHER, FileType.fromUri("other"));
		assertEquals(FileType.VIDEO, FileType.fromUri("fileType_video"));
		assertEquals(FileType.TEXT, FileType.fromUri("http://spdx.org/rdf/terms#fileType_text"));
	}

	@Test
	public void testChecksumAlgorithms() {
		assertEquals("http://spdx.org/rdf/terms#checksumAlgorithm_md5", Checksum.Algorithm.MD5.getUri());
		assertEquals("http://spdx.org/rdf/terms#checksumAlgorithm_sha1", Checksum.Algorithm.SHA1.getUri());
		assertEquals("http://spdx.org/rdf/terms#checksumAlgorithm_sha256", Checksum.Algorithm.SHA256.getUri());
		assertEquals(Checksum.Algorithm.MD5, Checksum.Algorithm.fromUri("http://spdx.org/rdf/terms#checksumAlgorithm_md5"));
		assertEquals(Checksum.Algorithm.SHA1, Checksum.Algorithm.fromUri("http://spdx.org/rdf/terms#checksumAlgorithm_sha1"));
		assertEquals(Checksum.Algorithm.SHA256, Checksum.Algorithm.fromUri("http://spdx.org/rdf/terms#checksumAlgorithm_sha256"));

	}

}
