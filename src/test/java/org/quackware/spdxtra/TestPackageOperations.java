package org.quackware.spdxtra;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.ImmutableList;
import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Test;
import org.quackware.spdxtra.LicenseList.ListedLicense;
import org.quackware.spdxtra.Read.Document;
import org.quackware.spdxtra.Read.Package;
import org.quackware.spdxtra.Write.ModelUpdate;
import org.quackware.spdxtra.model.License;
import org.quackware.spdxtra.model.Relationship;
import org.quackware.spdxtra.model.SpdxDocument;
import org.quackware.spdxtra.model.SpdxPackage;

public class TestPackageOperations {

	@Test
	public void testPackageRead() throws IOException {
		Dataset dataset = TestModelOperations.getDefaultDataSet();
		Iterable<SpdxPackage> packages = Read.getAllPackages(dataset);
		SpdxPackage pkg = Iterables.find(packages, (p) -> "SPDXRef-1".equals(p.getSpdxId()));

		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getCopyright());
		assertEquals("SPDX tools", pkg.getName());
		assertEquals("2.0.0-RC1", pkg.getVersionInfo().orElse("NO VERSION? AWWWWWW..."));
		assertEquals(12, Iterables.size(Package.getFiles(pkg)));
	}

	@Test
	public void testPackageFieldUpdates() {
		Dataset dataset = TestModelOperations.getDefaultDataSet();
		SpdxDocument doc = Document.get(dataset);
		List<Relationship> relationships = Lists
				.newLinkedList(Read.getRelationships(dataset, doc, Relationship.Type.DESCRIBES));
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
		pkg = (SpdxPackage) Read.getRelationships(dataset, doc, Relationship.Type.DESCRIBES).iterator().next()
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
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(update));

		pkg = new SpdxPackage(
				Read.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1").get());
		assertEquals("http://spdx.org/licenses/Apache-2.0", pkg.getPropertyAsResource(SpdxProperties.LICENSE_DECLARED).getURI());

		// Set the declared license to NONE
		update = Write.Package.declaredLicense(pkg, License.NONE);
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(update));
		// Look closer to the metal. Did we create a duplicate property?
		Resource pkgResource = Read
				.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1").get();
		StmtIterator stmtIt = pkgResource.listProperties(SpdxProperties.LICENSE_DECLARED);
		assertTrue("Missing declared license assignment.", stmtIt.hasNext());
		String licenseUri = stmtIt.next().getObject().asResource().getURI();
		assertEquals("http://spdx.org/rdf/terms#none", licenseUri);
		assertTrue("Duplicate declared license assignment.", !stmtIt.hasNext());
		
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
		updates.add(Write.New.document(baseUrl, documentSpdxId, "El documento fantastico!"));

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
		List<Relationship> relationships = ImmutableList.copyOf(Read.getRelationships(dataset, doc));
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

		/*
		 * Now, let's verify the inverse relationship
		 */
		List<Relationship> pkgRelationships = ImmutableList.copyOf(Read.getRelationships(dataset, pkg));
		assertEquals(1, pkgRelationships.size());
		Relationship pkgDescribedByDoc = pkgRelationships.get(0);
		assertEquals(Relationship.Type.DESCRIBED_BY, pkgDescribedByDoc.getType());
		assertEquals(doc, pkgDescribedByDoc.getRelatedElement());

	}

}
