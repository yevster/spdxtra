package com.yevster.spdxtra.model;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableSet;
import com.yevster.spdxtra.LicenseList;
import com.yevster.spdxtra.NoneNoAssertionOrValue.AbsentValue;
import com.yevster.spdxtra.Read;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.TestUtils;
import com.yevster.spdxtra.Write;
import com.yevster.spdxtra.Write.ModelUpdate;
import com.yevster.spdxtra.model.write.License;
import com.yevster.spdxtra.util.MiscUtils;

import junit.framework.Assert;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yevster
 */
public class CompoundLicenseTest {
	private static final String documentNamespace = "http://example.org/compoundLicenseTest";
	private static final String documentSpdxId = "SPDXRef-1";
	private static final String pkgSpdxId = "SPDXRef-2";
	private static final String pkgUri = documentNamespace + "#" + pkgSpdxId;

	private Dataset dataset;

	@Before
	public void setup() {
		this.dataset = DatasetFactory.createTxnMem();

		List<ModelUpdate> updates = new LinkedList<>();
		updates.add(Write.New.document(documentNamespace, documentSpdxId, "El documento fantastico!",
				Creator.tool("Testy McTestface")));
		updates.add(Write.Document.addDescribedPackage(documentNamespace, documentSpdxId, pkgSpdxId, "Good things"));
		Write.applyUpdatesInOneTransaction(dataset, updates);
	}

	@Test
	public void testCreateConjunctiveLicense() {

		License conjunctiveLicense = License.and(LicenseList.INSTANCE.getListedLicenseById("GPL-2.0").get(),
				LicenseList.INSTANCE.getListedLicenseById("Apache-2.0").get());
		assertNotNull(conjunctiveLicense);

		assertEquals("(GPL-2.0) AND (Apache-2.0)", conjunctiveLicense.getPrettyName());
		Write.applyUpdatesInOneTransaction(dataset, Write.Package.concludedLicense(pkgUri, conjunctiveLicense));

		Resource pkgResource = Read.lookupResourceByUri(dataset, pkgUri).get();
		Resource licenseResource = pkgResource.getProperty(SpdxProperties.LICENSE_CONCLUDED).getObject().asResource();

		Stream<Statement> redfProps = MiscUtils
				.toLinearStream(licenseResource.listProperties(SpdxProperties.LICENSE_MEMBER));

		assertEquals(Sets.newHashSet("http://spdx.org/licenses/GPL-2.0", "http://spdx.org/licenses/Apache-2.0"),
				redfProps.map(Statement::getObject).map(Object::toString).collect(Collectors.toSet()));

	}

	@Test
	// TODO: Follow up with SPDX group as to whether this is legal.
	public void testCreateEmptyConjunctiveAndDisjunctiveLicenses() {
		SpdxPackage pkg = new SpdxPackage(Read.lookupResourceByUri(dataset, pkgUri).get());
		License conjunctiveLicense = License.and();
		License disjunctiveLicense = License.or();

		Write.applyUpdatesInOneTransaction(dataset, Write.Package.declaredLicense(pkg, disjunctiveLicense),
				Write.Package.concludedLicense(pkg, conjunctiveLicense));

	}

	@Test
	// TODO: Follow up with SPDX group as to whether this is legal.
	public void testCreateOneElementConjunctiveAndDisjunctiveLicenses() {
		SpdxPackage pkg = new SpdxPackage(Read.lookupResourceByUri(dataset, pkgUri).get());
		License conjunctiveLicense = License.and(LicenseList.INSTANCE.getListedLicenseById("Apache-2.0").get());
		License disjunctiveLicense = License.or(License.NOASSERTION);

		Write.applyUpdatesInOneTransaction(dataset, Write.Package.declaredLicense(pkg, disjunctiveLicense),
				Write.Package.concludedLicense(pkg, conjunctiveLicense));

	}

	@Test
	public void testDisjunctiveLicense() {
		String extractedLicenseId = "LicenseRef-AyCaramba";
		License disjunctiveLicense = License.or(License.extracted("Ay caramba, dios mio! No estoy bien.", "oyoyoyo",
				documentNamespace, extractedLicenseId), License.NOASSERTION);

		assertNotNull(disjunctiveLicense);
		assertEquals("(oyoyoyo) OR (NOASSERTION)", disjunctiveLicense.getPrettyName());

		Write.applyUpdatesInOneTransaction(dataset, Write.Package.declaredLicense(pkgUri, disjunctiveLicense));

		Resource pkgResource = Read.lookupResourceByUri(dataset, pkgUri).get();
		Resource licenseResource = pkgResource.getProperty(SpdxProperties.LICENSE_DECLARED).getObject().asResource();

		Stream<Statement> rdfProps = MiscUtils
				.toLinearStream(licenseResource.listProperties(SpdxProperties.LICENSE_MEMBER));
		assertEquals(Sets.newHashSet(AbsentValue.NOASSERTION.getUri(), documentNamespace + "#" + extractedLicenseId),
				rdfProps.map(Statement::getObject).map(Object::toString).collect(Collectors.toSet()));
	}

	@Test
	// Make sure a single compound license does not create multiple resources in
	// the document
	public void compoundLicenseIdempotenceTest() {
		final String file1id = "SPDXRef-File1";
		final String file2id = "SPDXRef-File1PSYCH";
		final String extractedLicenseId = "LicenseRef-AyCaramba";
		License disjunctiveLicense = License.or(License.extracted("Ay caramba, dios mio! No estoy bien.", "oyoyoyo",
				documentNamespace, extractedLicenseId), License.NOASSERTION);
		Write.applyUpdatesInOneTransaction(dataset,
				// Two files, give each the same license,
				// so that if it's broken, it spawns two resources
				Write.Package.addFile(documentNamespace, pkgSpdxId, file1id, "file1.exe"),
				Write.Package.addFile(documentNamespace, pkgSpdxId, file2id, "file2.dat"),
				Write.File.concludedLicense(documentNamespace + "#" + file1id, disjunctiveLicense),
				Write.File.concludedLicense(documentNamespace + "#" + file2id, disjunctiveLicense));
		// Get all the compound license resources
		List<Resource> compoundLicenseResources = MiscUtils
				.toLinearStream(dataset.getDefaultModel().listResourcesWithProperty(SpdxProperties.LICENSE_MEMBER))
				.collect(Collectors.toList());
		dataset.getDefaultModel().write(System.out);
		// There should only be one.
		assertEquals(1, compoundLicenseResources.size());

		// Make sure we don't get errors applying the same license to a
		// different model (unlikely edge case, but...)
		Dataset dataset2 = TestUtils.getDefaultDataSet();
		final String dataset2FileUri = "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-151";
		Write.applyUpdatesInOneTransaction(dataset2, Write.File.concludedLicense(dataset2FileUri, disjunctiveLicense));

		dataset2.begin(ReadWrite.READ);
		Resource fileResource = dataset2.getDefaultModel().getResource(dataset2FileUri);
		List<Resource> concludedLicenseResources = MiscUtils
				.toLinearStream(fileResource.listProperties(SpdxProperties.LICENSE_CONCLUDED)).map(Statement::getObject)
				.map(RDFNode::asResource).collect(Collectors.toList());
		assertEquals(1, concludedLicenseResources.size());
		try {
			dataset2.getDefaultModel().write(Files.newOutputStream(Paths.get("/var/tmp/out.rdf")));
		} catch (IOException e) {
		}
		// Make sure it's our trusty compound license
		assertTrue(concludedLicenseResources.get(0).listProperties(SpdxProperties.LICENSE_MEMBER).hasNext());

	}

}
