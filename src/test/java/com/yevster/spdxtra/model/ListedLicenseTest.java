package com.yevster.spdxtra.model;

import static org.junit.Assert.assertNotNull;

import java.util.Optional;

import org.junit.Test;

import com.yevster.spdxtra.LicenseList;
import com.yevster.spdxtra.LicenseList.ListedLicense;

import static org.junit.Assert.*;

public class ListedLicenseTest {
	@Test
	public void testRetrieval() {
		Optional<ListedLicense> license = LicenseList.INSTANCE.getListedLicenseById("Apache-2.0");
		assertNotNull(license);
		assertTrue(license.isPresent());
		assertEquals("Apache License 2.0", license.get().getName());
		assertEquals("Apache-2.0", license.get().getLicenseId());
		assertTrue(license.get().isOsiApproved());
		// Make sure the license does not get fetched twice
		assertTrue(license.get() == LicenseList.INSTANCE.getListedLicenseById("Apache-2.0").get());
	}

	@Test
	public void testRetrievalNonexistingLicense() {
		Optional<ListedLicense> noSuchLicense = LicenseList.INSTANCE.getListedLicenseById("GPL-non-idiotic");
		assertEquals(Optional.empty(), noSuchLicense);
	}
}
