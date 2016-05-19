package com.yevster.spdxtra.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.ext.com.google.common.collect.Sets;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.Before;
import org.junit.Test;

import com.yevster.spdxtra.Read;
import com.yevster.spdxtra.SpdxProperties;
import com.yevster.spdxtra.Write;
import com.yevster.spdxtra.Write.ModelUpdate;
import com.yevster.spdxtra.model.write.License;
import com.yevster.spdxtra.util.MiscUtils;

/**
 * @author yevster
 */
public class ExtractedLicenseTest {
	private static final String documentNamespace = "http://example.org/compoundLicenseTest";
	private static final String documentSpdxId = "SPDXRef-1";
	private static final String pkgSpdxId = "SPDXRef-2";
	private static final String pkgUri = documentNamespace + "#" + pkgSpdxId;

	private Dataset dataset;

	@Before
	public void setup() {
		this.dataset = DatasetFactory.createTxnMem();

		List<ModelUpdate> updates = new LinkedList<>();
		updates.add(Write.New.document(documentNamespace, documentSpdxId, "El documento fantastico!", Creator.tool("Testy McTestface")));
		updates.add(Write.Document.addDescribedPackage(documentNamespace, documentSpdxId, pkgSpdxId, "Good things"));
		Write.applyUpdatesInOneTransaction(dataset, updates);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateIllegalText() {
		License.extracted("   \n", "namey", "http://foo", "bar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateIllegalSpdxId() {
		License.extracted("This is my license. Enjoy.", "namey", "http://foo", "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateIllegalBaseUrl() {
		License.extracted("This is my license. Enjoy.", "namey", "http://foo#", "bar");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateIllegalName() {
		License.extracted("This is my license. Enjoy.", "  \n", "http://foo#", "bar");
	}

	@Test
	public void testAssignExtracted() {
		final String licenseText = "this is my license.\nCopyright(c) 2016 Awesome Corporation of Awesomness, Inc. Ltd. Bbc. Ddt.";
		final String licenseName = "namey";
		String spdxId = "LicenseRef-141";
		License extractedLicense = License.extracted(licenseText, licenseName, documentNamespace, spdxId);
		assertNotNull(extractedLicense);

		// We're going to apply the update twice, to make sure the second time
		// the properties do not get duplicated.

		for (int i = 0; i < 2; ++i) {
			Write.applyUpdatesInOneTransaction(dataset, Write.Package.declaredLicense(pkgUri, extractedLicense));

			Resource pkgResource = Read.lookupResourceByUri(dataset, pkgUri).get();
			Resource licenseResource = pkgResource.getProperty(SpdxProperties.LICENSE_DECLARED).getObject().asResource();

			assertEquals(documentNamespace + "#" + spdxId, licenseResource.getURI());
			Stream<Statement> rdfProps = MiscUtils.toLinearStream(licenseResource.listProperties(SpdxProperties.LICENSE_ID));
			assertEquals(Sets.newHashSet(spdxId), rdfProps.map(Statement::getObject).map(Object::toString).collect(Collectors.toSet()));
			rdfProps = MiscUtils.toLinearStream(licenseResource.listProperties(SpdxProperties.LICENSE_EXTRACTED_TEXT));
			assertEquals(Sets.newHashSet(licenseText),
					rdfProps.map(Statement::getObject).map(Object::toString).collect(Collectors.toSet()));
			rdfProps = MiscUtils.toLinearStream(licenseResource.listProperties(SpdxProperties.NAME));
			assertEquals(Sets.newHashSet(licenseName),
					rdfProps.map(Statement::getObject).map(Object::toString).collect(Collectors.toSet()));

		}

	}

}
