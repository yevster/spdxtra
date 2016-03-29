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

	public TestCreateDocumentBadInputs(String baseUrl, String spdxId) {
		this.baseUrl = baseUrl;
		this.spdxId = spdxId;
	}

	@Parameters
	public static Collection<Object[]> generateData() {
		return Arrays.asList(new Object[][] { 
			{ "http://foo#", "SPDXRef-bar" },
			{ "http://foo", "SPDXRef-#bar"},
			{ "http://foo", "   "},
			{ " ", "SPDXRef-bar"},
			{ "foo", "bar"}
		
		});
	}


	@Test(expected = IllegalArgumentException.class)
	public void testDocumentHashInBase() {
		Write.New.document(baseUrl, spdxId);
	}


}
