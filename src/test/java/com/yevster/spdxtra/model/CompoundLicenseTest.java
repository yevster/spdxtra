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

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.Before;
import org.junit.Test;

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
		License disjunctiveLicense = License.or(
				License.extracted("Ay caramba, dios mio! No estoy bien.", documentNamespace, extractedLicenseId),
				License.NOASSERTION);
		assertNotNull(disjunctiveLicense);

		Write.applyUpdatesInOneTransaction(dataset, Write.Package.declaredLicense(pkgUri, disjunctiveLicense));

		Resource pkgResource = Read.lookupResourceByUri(dataset, pkgUri).get();
		Resource licenseResource = pkgResource.getProperty(SpdxProperties.LICENSE_DECLARED).getObject().asResource();

		Stream<Statement> rdfProps = MiscUtils
				.toLinearStream(licenseResource.listProperties(SpdxProperties.LICENSE_MEMBER));
		assertEquals(Sets.newHashSet(AbsentValue.NOASSERTION.getUri(), documentNamespace + "#" + extractedLicenseId),
				rdfProps.map(Statement::getObject).map(Object::toString).collect(Collectors.toSet()));

	}

}
