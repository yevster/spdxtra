package com.yevster.spdxtra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.yevster.spdxtra.Read;
import com.yevster.spdxtra.Write;
import com.yevster.spdxtra.LicenseList.ListedLicense;
import com.yevster.spdxtra.Write.ModelUpdate;
import com.yevster.spdxtra.model.Creator;
import com.yevster.spdxtra.model.SpdxDocument;

import org.apache.jena.atlas.lib.SetUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Test;

public class TestDocumentOperations {

	@Test
	public void createDocument() {
		final String id = "SPDXRef-Wickendess";
		final String baseUrl = "http://example.org";
		final String name = "Que paso?";
		Dataset dataset = DatasetFactory.createTxnMem();

		ModelUpdate creation = Write.New.document(baseUrl, id, name,
				Creator.person("Albert Einstein", Optional.of("aEinstein@princeton.edu")), Creator.tool("SpdXtra"),
				Creator.organization("Aperture Science, LLC.", Optional.empty()));

		assertNotNull(creation);
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(creation));

		SpdxDocument document = Read.Document.get(dataset);
		Resource documentResource = Read.lookupResourceByUri(dataset, baseUrl + "#" + id).get();
		assertNotNull(document);
		assertEquals(baseUrl + "#" + id, document.getUri());
		assertEquals(id, document.getSpdxId());
		assertEquals(name, document.getName());

		// The data license should have no properties, just the URI;
		assertEquals("http://spdx.org/licenses/CC0-1.0", document.getPropertyAsResource(SpdxProperties.DATA_LICENSE).getURI());
		// Verify creation Info
		Resource creationInfo = document.getPropertyAsResource(SpdxProperties.CREATION_INFO);
		assertNotNull(creationInfo);
		StmtIterator creatorStatements = creationInfo.listProperties(SpdxProperties.CREATOR);
		Set<String> creatorStrings = stmtIteratorToStringSet(creatorStatements);

		Set<String> expectedCreators = ImmutableSet.of("Person: Albert Einstein (aEinstein@princeton.edu)", "Tool: SpdXtra",
				"Organization: Aperture Science, LLC. ()");
		assertEquals("Unexpected creator info", 0, Sets.symmetricDifference(expectedCreators, creatorStrings).size());

	}

	private static Set<String> stmtIteratorToStringSet(StmtIterator it) {
		Set<String> result = new HashSet<>();
		while (it.hasNext()) {
			result.add(it.next().getString());
		}
		return result;

	}
}
