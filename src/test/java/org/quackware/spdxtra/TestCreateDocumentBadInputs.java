package org.quackware.spdxtra;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestCreateDocumentBadInputs {
	private String baseUrl;
	private String spdxId;
	private String name;

	public TestCreateDocumentBadInputs(String baseUrl, String spdxId, String name) {
		this.baseUrl = baseUrl;
		this.spdxId = spdxId;
		this.name = name;
	}

	@Parameters
	public static Collection<Object[]> generateData() {
		return Arrays.asList(new Object[][] { { "http://foo#", "SPDXRef-bar", "goodname" }, { "http://foo", "SPDXRef-#bar", "goodname" },
				{ "http://foo", "   ", "goodname" }, { " ", "SPDXRef-bar", "goodname" }, { "foo", "bar", "goodname" },
				{ "ladeda://foo", "bar", "nice\nline" }, { "ladeda://foo", "bar", "    " }

		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDocumentHashInBase() {
		Write.New.document(baseUrl, spdxId, name);
	}

}
