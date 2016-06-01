package com.yevster.spdxtra.perftest;

import java.util.Optional;
import java.util.Random;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.junit.Test;

import com.yevster.spdxtra.LicenseList;
import com.yevster.spdxtra.NoneNoAssertionOrValue;
import com.yevster.spdxtra.Write;
import com.yevster.spdxtra.model.Creator;
import com.yevster.spdxtra.model.FileType;
import com.yevster.spdxtra.model.Relationship;
import com.yevster.spdxtra.model.write.License;

/**
 * This test can be run to ensure that performance with a large number of files
 * inside a package is tractible. This test should not run in the unit test
 * suite or as part of a CI build.
 * 
 * @author yevster
 *
 */

public class PerformanceWithFilesInsideOnePackageTest {
	private static final String baseUrl = "http://example.com/spdxPerfTest";

	private static final License[] LICENSES = new License[] { License.NOASSERTION, License.NONE,
			LicenseList.INSTANCE.getListedLicenseById("Apache-2.0").get(),
			LicenseList.INSTANCE.getListedLicenseById("MIT").get(),
			License.extracted(RandomStringUtils.randomAlphabetic(300), "Random Extracted License", baseUrl,
					"LicenseRef-RNDExtr") };

	@Test
	public void test10000filesInMemory() {
		final int fileCount = 40 * 1000;
		Dataset dataset = DatasetFactory.createTxnMem();

		String docSpdxId = "SPDXRef-doc";
		String pkgSpdxId = "SPDXRef-pkg";
		String pkgUri = baseUrl + "#" + pkgSpdxId;

		StopWatch sw = new StopWatch();
		sw.start();

		// Create the base document.
		Write.applyUpdatesInOneTransaction(dataset,
				Write.New.document(baseUrl, docSpdxId, "El Docco", Creator.person("God", Optional.empty()),
						Creator.tool("superpowers")),
				Write.Document.addDescribedPackage(baseUrl, docSpdxId, pkgSpdxId, "The package of testing"),
				Write.Package.packageDownloadLocation(pkgUri, NoneNoAssertionOrValue.of("http://example.org/testpkg")));
		System.out.println("Writing files...");
		for (int i = 0; i < fileCount; ++i) {
			String fileSpdxId = "SPDXRef-" + i;
			String fileUri = baseUrl + "#" + fileSpdxId;
			// Every 5th file has a giant license in it.
			License licenseFromFile = i % 5 == 4 ? License.extracted(RandomStringUtils.randomAscii(2048), "Random " + i,
					baseUrl, "LicenseRef-ayayay" + i) : License.NONE;

			Write.applyUpdatesInOneTransaction(dataset,

					Write.Package.addFile(baseUrl, pkgSpdxId, fileSpdxId,
							"./" + RandomStringUtils.randomAlphabetic(15) + "."
									+ RandomStringUtils.randomAlphabetic(3)),
					Write.File.artifactOf(fileUri, RandomStringUtils.randomAlphanumeric(RandomUtils.nextInt(3, 30)),
							"http://" + RandomStringUtils.randomAlphabetic(10) + ".com"),
					Write.File.fileTypes(fileUri, FileType.OTHER),
					Write.File.concludedLicense(fileUri, LICENSES[i % LICENSES.length]),
					Write.File.addLicenseInfoInFile(fileUri, licenseFromFile),
					Write.File.copyrightText(fileUri, NoneNoAssertionOrValue.of("Copyright (C) 2016 "+RandomStringUtils.randomAscii(RandomUtils.nextInt(1, 50)))),
					Write.File.comment(fileUri, RandomStringUtils.randomAlphanumeric(RandomUtils.nextInt(0, 30))),
					Write.addRelationship(pkgUri, fileUri, Optional.empty(), Relationship.Type.CONTAINS)
					//Write.addRelationship(fileUri, pkgUri, Optional.empty(), Relationship.Type.CONTAINED_BY)
					);
			
		}
		System.out.print(sw.getTime());
		System.out.println(" : Generated files. Computing verification code.");
		Write.applyUpdatesInOneTransaction(dataset, Write.Package.finalize(pkgUri));
		System.out.print(sw.getTime());
		System.out.println("  Writing document (to null stream)");
		dataset.getDefaultModel().write(new NullOutputStream());
		sw.stop();
		System.out.println("total time: " + sw.getTime());

	}

}
