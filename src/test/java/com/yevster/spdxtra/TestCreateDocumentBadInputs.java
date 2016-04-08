package com.yevster.spdxtra;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.yevster.spdxtra.model.Creator;

@RunWith(Parameterized.class)
public class TestCreateDocumentBadInputs {
	private String baseUrl;
	private String spdxId;
	private String name;
	private Creator creator;
	private static final Creator goodCreator = Creator.tool("SpdXtra");
	private static final String goodId = "SPDXRef-666";

	public TestCreateDocumentBadInputs(String baseUrl, String spdxId, String name, Creator creator) {
		this.baseUrl = baseUrl;
		this.spdxId = spdxId;
		this.name = name;
		this.creator = creator;
	}

	@Parameters
	public static Collection<Object[]> generateData() {
		return Arrays.asList(new Object[][] {
				// Bad baseUrl or ID:

				{ "http://foo#", "SPDXRef-bar", "goodname", goodCreator }, { "http://foo", "SPDXRef-#bar", "goodname", goodCreator },
				{ "http://foo", "   ", "goodname", goodCreator }, { " ", "SPDXRef-bar", "goodname", goodCreator },
				{ "foo", "bar", "goodname", goodCreator },

				// Bad name:
				{ "ladeda://foo", "bar", "nice\nline", goodCreator }, { "ladeda://foo", "bar", "    ", goodCreator },

		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDocumentHashInBase() {
		Write.New.document(baseUrl, spdxId, name, creator);
	}

	@Test
	public void testIllegalCreatorInitializations() {
		// Bad creator
		/**
		 * { "goodBase", goodId, "Good Name", null }, { "goodBase", goodId,
		 * "Good Name", Creator.person("  \t  ", Optional.empty()) }, {
		 * "goodBase", goodId, "Good Name", Creator.organization("FooCo",
		 * Optional.of("no pasaran")) },
		 * 
		 * }
		 **/
		ensureIllegalArgumentExceptionInCreator(() -> Creator.person(null, Optional.of("barack@whitehouse.gov")));
		ensureIllegalArgumentExceptionInCreator(() -> Creator.person("Good name", Optional.of("bad email")));
		ensureIllegalArgumentExceptionInCreator(() -> Creator.tool("  \t    "));
	}

	public void ensureIllegalArgumentExceptionInCreator(Supplier<Creator> supplier) {
		try {
			supplier.get();
			Assert.fail("Expected Illegal argument exception");
		} catch (Exception e) {
			assertTrue("Incorrect exception type: " + e.getClass().getName(), e instanceof IllegalArgumentException);
		}

	}

}
