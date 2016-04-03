package com.yevster.spdxtra;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import com.yevster.spdxtra.model.Creator;
import com.yevster.spdxtra.model.License;
import com.yevster.spdxtra.model.Relationship;
import com.yevster.spdxtra.model.SpdxDocument;
import com.yevster.spdxtra.model.SpdxFile;
import com.yevster.spdxtra.model.SpdxPackage;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Test;

public class TestPackageOperations {

	@Test
	public void testPackageRead() throws IOException {
		Dataset dataset = TestModelOperations.getDefaultDataSet();
		Stream<SpdxPackage> packages = Read.getAllPackages(dataset);
		SpdxPackage pkg = packages.filter((p) -> "SPDXRef-1".equals(p.getSpdxId())).findFirst().get();

		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getCopyright());
		assertEquals("SPDX tools", pkg.getName());
		assertEquals("2.0.0-RC1", pkg.getVersionInfo().orElse("NO VERSION? AWWWWWW..."));
		List<SpdxFile> files = Package.getFiles(pkg).collect(Collectors.toList());
		assertEquals(12, files.size());
	}

	@Test
	public void testPackageFieldUpdates() {
		Dataset dataset = TestModelOperations.getDefaultDataSet();
		SpdxDocument doc = Document.get(dataset);
		List<Relationship> relationships = Read.getRelationships(dataset, doc, Relationship.Type.DESCRIBES)
				.collect(Collectors.toList());
		assertEquals(1, relationships.size());
		assertEquals(SpdxPackage.class, relationships.get(0).getRelatedElement().getClass());
		SpdxPackage pkg = (SpdxPackage) relationships.get(0).getRelatedElement();

		List<RdfResourceUpdate> updates = new LinkedList<>();
		// Let's update the name
		final String newName = "Definitely not the old name";
		final String oldName = pkg.getName();
		assertEquals("SPDX tools", oldName);
		RdfResourceUpdate update = Write.Package.name(pkg.getUri(), newName);
		// The package should not have been changed.
		assertEquals(oldName, pkg.getName());
		assertEquals(pkg.getUri(), update.getResourceUri());
		updates.add(update);

		// Apply the updates
		Write.applyUpdatesInOneTransaction(dataset, updates);

		// Reload the package from the document model
		pkg = (SpdxPackage) Read.getRelationships(dataset, doc, Relationship.Type.DESCRIBES).findFirst().get()
				.getRelatedElement();
		assertEquals(newName, pkg.getName());
	}

	@Test
	public void testPackageLicenseUpdates() {
		Dataset dataset = TestModelOperations.getDefaultDataSet();
		SpdxPackage pkg = new SpdxPackage(
				Read.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1").get());
		// Let's set a declared license.
		RdfResourceUpdate update = Write.Package.declaredLicense(pkg,
				LicenseList.INSTANCE.getListedLicenseById("Apache-2.0").get());
		assertEquals(SpdxProperties.LICENSE_DECLARED, update.getProperty());

		// And a concluded license to NOASSERT
		RdfResourceUpdate update2 = Write.Package.concludedLicense(pkg, License.NOASSERTION);
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(update, update2));

		pkg = new SpdxPackage(
				Read.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1").get());
		assertEquals("http://spdx.org/licenses/Apache-2.0",
				pkg.getPropertyAsResource(SpdxProperties.LICENSE_DECLARED).getURI());
		assertEquals("http://spdx.org/rdf/terms#noassertion",
				pkg.getPropertyAsResource(SpdxProperties.LICENSE_CONCLUDED).getURI());

		// Set the declared license to NONE and concluded license to GPL-2.0
		update = Write.Package.declaredLicense(pkg, License.NONE);
		update2 = Write.Package.concludedLicense(pkg, LicenseList.INSTANCE.getListedLicenseById("GPL-2.0").get());
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(update, update2));

		// Look closer to the metal. Did we create a duplicate property...
		Resource pkgResource = Read
				.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1").get();
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
		updates.add(Write.New.document(baseUrl, documentSpdxId, "El documento fantastico!",
				Creator.tool("Testy McTestface")));

		if (separateTransactions) {
			Write.applyUpdatesInOneTransaction(dataset, updates);
			updates.clear();
		}

		updates.add(Write.Document.addDescribedPackage(baseUrl, documentSpdxId, packageSpdxId, packageName));
		if (separateTransactions) {
			Write.applyUpdatesInOneTransaction(dataset, updates);
			updates.clear();
		}

		updates.add(
				Write.Package.copyrightText(baseUrl + "#" + packageSpdxId, NoneNoAssertionOrValue.of(copyrightText)));
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

}
