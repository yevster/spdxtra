package com.yevster.spdxtra;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.yevster.spdxtra.LicenseList;
import com.yevster.spdxtra.NoneNoAssertionOrValue;
import com.yevster.spdxtra.RdfResourceUpdate;
import com.yevster.spdxtra.Read;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.Write;
import com.yevster.spdxtra.Read.Document;
import com.yevster.spdxtra.Read.Package;
import com.yevster.spdxtra.Write.ModelUpdate;
import com.yevster.spdxtra.model.Checksum;
import com.yevster.spdxtra.model.Creator;
import com.yevster.spdxtra.model.Creator.HumanCreator;
import com.yevster.spdxtra.model.Relationship;
import com.yevster.spdxtra.model.SpdxDocument;
import com.yevster.spdxtra.model.SpdxFile;
import com.yevster.spdxtra.model.SpdxPackage;
import com.yevster.spdxtra.model.write.License;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Test;

public class TestPackageOperations {

	@Test
	public void testPackageRead() throws IOException {
		Dataset dataset = TestUtils.getDefaultDataSet();
		Stream<SpdxPackage> packages = Read.getAllPackages(dataset);
		SpdxPackage pkg = packages.filter((p) -> "SPDXRef-1".equals(p.getSpdxId())).findFirst().get();

		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getCopyright());
		assertEquals("SPDX tools", pkg.getName());
		assertEquals("2.0.0-RC1", pkg.getVersionInfo().orElse("NO VERSION? AWWWWWW..."));
		assertEquals("Expected filesAnalyzed to be true when not specified.", true, pkg.getFilesAnalyzed());
		assertEquals("spdx-tools.jar", pkg.getPackageFileName().get());
		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getPackageDownloadLocation());

		List<SpdxFile> files = pkg.getFiles().collect(Collectors.toList());
		assertEquals(578, files.size());
	}

	@Test
	public void testPackageFieldUpdates() {
		Dataset dataset = TestUtils.getDefaultDataSet();
		SpdxDocument doc = Document.get(dataset);
		List<Relationship> relationships = Read.getRelationships(dataset, doc, Relationship.Type.DESCRIBES).collect(Collectors.toList());
		assertEquals(1, relationships.size());
		assertEquals(SpdxPackage.class, relationships.get(0).getRelatedElement().getClass());
		SpdxPackage pkg = (SpdxPackage) relationships.get(0).getRelatedElement();

		List<ModelUpdate> updates = new LinkedList<>();
		// Let's update the name
		final String newName = "Definitely not the old name";
		final String oldName = pkg.getName();
		assertEquals("SPDX tools", oldName);
		ModelUpdate update = Write.Package.name(pkg.getUri(), newName);
		// The package should not have been changed.
		assertEquals(oldName, pkg.getName());
		//This is a unit test, so we'll break encapsulation to look inside:
		assertEquals(pkg.getUri(), ((RdfResourceUpdate)update).getResourceUri());
		updates.add(Write.Package.filesAnalyzed(pkg.getUri(), false));
		updates.add(Write.Package.licenseComments(pkg.getUri(), "Nice license!"));
		updates.add(Write.Package.comment(pkg.getUri(), "Package comment, yeeeah!"));
		updates.add(Write.Package.supplier(pkg.getUri(), Creator.organization("Evil, Inc.",Optional.empty())));
		updates.add(Write.Package.originator(pkg.getUri(), Creator.person("Orey Ginator", Optional.of("oreyg@example.com"))));
		updates.add(Write.Package.checksums(pkg.getUri(), "abc123"));
		updates.add(update);

		final String packageDownloadLocation = "git+https://git.myproject.org/MyProject.git@v10.0#src/lib.c";
		updates.add(Write.Package.packageDownloadLocation(pkg.getUri(), NoneNoAssertionOrValue.of(packageDownloadLocation)));

		final String version = "6.6.6-alpha-1";
		updates.add(Write.Package.version(pkg.getUri(), version));

		final String packageHomePage = "http://www.example.org/packageOfDoom";
		updates.add(Write.Package.homepage(pkg.getUri(), NoneNoAssertionOrValue.of(packageHomePage)));
		
		final String summary = "This is a summary";
		final String description = "This is a detailed description. It's even more boring than the summary.";
		final String sourceInfo = "This is the source info. Use the source, Luke!";
		
		updates.add(Write.Package.description(pkg.getUri(), description));
		updates.add(Write.Package.summary(pkg.getUri(), summary));
		updates.add(Write.Package.sourceInfo(pkg.getUri(), sourceInfo));

		// Apply the updates
		Write.applyUpdatesInOneTransaction(dataset, updates);

		// Reload the package from the document model

		pkg = (SpdxPackage) Read.getRelationships(dataset, doc, Relationship.Type.DESCRIBES).findFirst().get().getRelatedElement();

		assertEquals(newName, pkg.getName());
		assertEquals(false, pkg.getFilesAnalyzed());
		assertEquals(packageDownloadLocation, pkg.getPackageDownloadLocation().getValue().orElse("YOU FAIL!"));
		assertEquals(packageHomePage, pkg.getHomepage().getValue().orElse("YOU FAIL!"));
		assertEquals(Optional.of(version), pkg.getVersionInfo());
		assertEquals("Nice license!", pkg.getOptionalPropertyAsString(SpdxProperties.LICENSE_COMMENTS).get());
		assertEquals(Optional.of(description), pkg.getDescription());
		assertEquals(Optional.of(summary), pkg.getSummary());
		assertEquals(Optional.of(sourceInfo), pkg.getSourceInfo());
		assertEquals(Optional.of("Package comment, yeeeah!"), pkg.getComment());
		assertEquals(Optional.of("Organization: Evil, Inc. ()"), pkg.getSupplier());
		assertEquals(Optional.of("Person: Orey Ginator (oreyg@example.com)"), pkg.getOriginator());
		assertEquals(Sets.newHashSet(Checksum.sha1("abc123")), pkg.getChecksums());
	}

	@Test
	public void testPackageLicenseUpdates() {
		Dataset dataset = TestUtils.getDefaultDataSet();
		SpdxPackage pkg = new SpdxPackage(
				Read.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1").get());
		// Let's set a declared license.
		RdfResourceUpdate update = (RdfResourceUpdate)Write.Package.declaredLicense(pkg, LicenseList.INSTANCE.getListedLicenseById("Apache-2.0").get());
		assertEquals(SpdxProperties.LICENSE_DECLARED, update.getProperty());

		// And a concluded license to NOASSERT
		ModelUpdate update2 = Write.Package.concludedLicense(pkg, License.NOASSERTION);
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(update, update2));

		pkg = new SpdxPackage(Read.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1").get());
		assertEquals("http://spdx.org/licenses/Apache-2.0", pkg.getPropertyAsResource(SpdxProperties.LICENSE_DECLARED).get().getURI());
		assertEquals("http://spdx.org/rdf/terms#noassertion", pkg.getPropertyAsResource(SpdxProperties.LICENSE_CONCLUDED).get().getURI());

		// Set the declared license to NONE and concluded license to GPL-2.0
		
		Write.applyUpdatesInOneTransaction(dataset, 
				Write.Package.declaredLicense(pkg, License.NONE),
				Write.Package.concludedLicense(pkg, LicenseList.INSTANCE.getListedLicenseById("GPL-2.0").get()));

		// Look closer to the metal. Did we create a duplicate property...
		Resource pkgResource = Read.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1").get();
		// ...for declared?
		StmtIterator stmtIt = pkgResource.listProperties(SpdxProperties.LICENSE_DECLARED);
		assertTrue("Missing declared license assignment.", stmtIt.hasNext());
		String licenseUri = stmtIt.next().getObject().asResource().getURI();
		assertEquals("http://spdx.org/rdf/terms#none", licenseUri);
		assertTrue("Duplicate declared license assignment.", !stmtIt.hasNext());
		// ...for concluded?
		stmtIt = pkgResource.listProperties(SpdxProperties.LICENSE_CONCLUDED);
		assertTrue("Missing concluded license assignment.", stmtIt.hasNext());
		licenseUri = stmtIt.next().getObject().asResource().getURI();
		assertEquals("http://spdx.org/licenses/GPL-2.0", licenseUri);
		assertTrue("Duplicate concluded license assignment.", !stmtIt.hasNext());
	}

	@Test
	public void testDefaultFieldValues() {
		Dataset dataset = DatasetFactory.createTxnMem();
		final String baseUrl = "http://example.com";
		final String documentSpdxId = "SPDXRef-MyDoc";
		final String packageSpdxId = "SPDXRef-MyBundleOfJoy";
		final String packageName = "BundleOfJoy";

		List<ModelUpdate> updates = new LinkedList<>();
		updates.add(Write.New.document(baseUrl, documentSpdxId, "El documento fantastico!", Creator.tool("Testy McTestface")));
		updates.add(Write.Document.addPackage(baseUrl, documentSpdxId, packageSpdxId, packageName));
		Write.applyUpdatesInOneTransaction(dataset, updates);

		SpdxPackage pkg = new SpdxPackage(Read.lookupResourceByUri(dataset, baseUrl + "#" + packageSpdxId).get());

		// TESTING STARTS HERE
		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getCopyright());
		assertEquals(Optional.empty(), pkg.getPackageFileName());
		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getPackageDownloadLocation());
		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getHomepage());
		assertEquals("New package should have no relationships", 0, Read.getRelationships(dataset, pkg).count());
		assertEquals("A package should not have a version by default.", Optional.empty(), pkg.getVersionInfo());

	}

	@Test
	public void testPkgFromScratch1Transaction() {
		testPackageFromScratch(false);
	}

	@Test
	public void testPkgFromScratchMultipleTransactions() {
		testPackageFromScratch(true);
	}

	public void testPackageFromScratch(boolean separateTransactions) {
		Dataset dataset = DatasetFactory.createTxnMem();
		final String baseUrl = "http://example.com";
		final String documentSpdxId = "SPDXRef-MyDoc";
		final String packageSpdxId = "SPDXRef-MyBundleOfJoy";
		final String packageName = "BundleOfJoy";
		final String copyrightText = "Copyright(c) 2016 Joyco, Inc.\nAll rights reserved.\nSo don'tcha be messin'";

		List<ModelUpdate> updates = new LinkedList<>();
		updates.add(Write.New.document(baseUrl, documentSpdxId, "El documento fantastico!", Creator.tool("Testy McTestface")));

		if (separateTransactions) {
			Write.applyUpdatesInOneTransaction(dataset, updates);
			updates.clear();
		}

		updates.add(Write.Document.addDescribedPackage(baseUrl, documentSpdxId, packageSpdxId, packageName));
		if (separateTransactions) {
			Write.applyUpdatesInOneTransaction(dataset, updates);
			updates.clear();
		}

		updates.add(Write.Package.copyrightText(baseUrl + "#" + packageSpdxId, NoneNoAssertionOrValue.of(copyrightText)));
		Write.applyUpdatesInOneTransaction(dataset, updates);

		/*
		 * Now let's make sure the relationship got properly created!
		 */
		SpdxDocument doc = new SpdxDocument(Read.lookupResourceByUri(dataset, baseUrl + "#" + documentSpdxId).get());
		assertNotNull(doc);
		List<Relationship> relationships = Read.getRelationships(dataset, doc).collect(Collectors.toList());
		assertEquals(1, relationships.size());

		Relationship docDescribesPackage = relationships.get(0);
		assertEquals(Relationship.Type.DESCRIBES, docDescribesPackage.getType());
		assertTrue(docDescribesPackage.getRelatedElement() instanceof SpdxPackage);

		/*
		 * Now, let's verify the package
		 */
		SpdxPackage pkg = (SpdxPackage) docDescribesPackage.getRelatedElement();
		assertEquals(packageName, pkg.getName());
		assertEquals(packageSpdxId, pkg.getSpdxId());
		assertEquals(baseUrl + "#" + packageSpdxId, pkg.getUri());
		assertEquals(copyrightText, pkg.getCopyright().getLiteralOrUriValue());
		assertEquals(true, pkg.getFilesAnalyzed());// FilesAnalyzed should
		// default to true.
		assertNull(pkg.getPropertyAsString(SpdxProperties.LICENSE_CONCLUDED));
		/*
		 * Now, let's verify the inverse relationship
		 */
		List<Relationship> pkgRelationships = Read.getRelationships(dataset, pkg).collect(Collectors.toList());
		assertEquals(1, pkgRelationships.size());
		Relationship pkgDescribedByDoc = pkgRelationships.get(0);
		assertEquals(Relationship.Type.DESCRIBED_BY, pkgDescribedByDoc.getType());
		assertEquals(doc, pkgDescribedByDoc.getRelatedElement());

	}

	@Test
	public void testPackageVerificationCode() {
		final String file1id = "SPDXRef-File1";
		final String file1sha1 = "f1d2d2f924e986ac86fdf7b36c94bcdf32beec15";
		final String file2id = "SPDXRef-File2";
		final String file2sha1 = "e242ed3bffccdf271b7fbaf34ed72d089537b42f";
		final String expectedPackageVerificationCode = "a0a8c4c4fc7960d0edc670a724071e908c6cfc10";

		Dataset dataset = DatasetFactory.createTxnMem();
		final String baseUrl = "http://example.com";
		final String documentSpdxId = "SPDXRef-MyDoc";
		final String packageSpdxId = "SPDXRef-MyBundleOfJoy";
		final String packageName = "BundleOfJoy";

		List<ModelUpdate> updates = new LinkedList<>();
		updates.add(Write.New.document(baseUrl, documentSpdxId, "El documento fantastico!", Creator.tool("Testy McTestface")));
		updates.add(Write.Document.addPackage(baseUrl, documentSpdxId, packageSpdxId, packageName));
		Write.applyUpdatesInOneTransaction(dataset, updates);

		ModelUpdate[] pkgUpdates = new ModelUpdate[] { Write.Package.addFile(baseUrl, packageSpdxId, file1id, "./file1.txt"),
				Write.File.checksums(baseUrl + "#" + file1id, file1sha1),
				Write.Package.addFile(baseUrl, packageSpdxId, file2id, "./file2.exe"),
				Write.File.checksums(baseUrl + "#" + file2id, file2sha1), Write.Package.finalize(baseUrl + "#" + packageSpdxId) };
		Write.applyUpdatesInOneTransaction(dataset, pkgUpdates);

		// Fish out the package
		SpdxPackage pkg = new SpdxPackage(dataset.getDefaultModel().getResource(baseUrl + "#" + packageSpdxId));
		assertNotNull(pkg);
		assertEquals(expectedPackageVerificationCode, pkg.getPackageVerificationCode().get());

		// Set the filesAnalyzed to true explicitly, ensure the same result.
		Write.applyUpdatesInOneTransaction(dataset, Write.Package.filesAnalyzed(baseUrl + "#" + packageSpdxId, true),
				Write.Package.finalize(baseUrl + "#" + packageSpdxId));
		pkg = new SpdxPackage(dataset.getDefaultModel().getResource(baseUrl + "#" + packageSpdxId));
		assertEquals(expectedPackageVerificationCode, pkg.getPackageVerificationCode().get());

		// Remove the files and set filesAnalyzed to false. Make sure the
		// verification code is absent.
		Write.applyUpdatesInOneTransaction(dataset,
				// Remove files from package
				(m) -> {
					m.getResource(baseUrl + "#" + packageSpdxId).removeAll(SpdxProperties.HAS_FILE);
				}, //Set filesAnalyzed to false
				Write.Package.filesAnalyzed(baseUrl + "#" + packageSpdxId, false),
				//Recompute
				Write.Package.finalize(baseUrl + "#" + packageSpdxId));
		pkg = new SpdxPackage(dataset.getDefaultModel().getResource(baseUrl + "#" + packageSpdxId));
		assertEquals(Optional.empty(), pkg.getPackageVerificationCode());
		
	}

}
