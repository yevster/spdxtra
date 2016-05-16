package com.yevster.spdxtra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.yevster.spdxtra.Write.ModelUpdate;
import com.yevster.spdxtra.model.Creator;
import com.yevster.spdxtra.model.SpdxDocument;

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

		assertNotNull(document);
		assertEquals(baseUrl + "#" + id, document.getUri());
		assertEquals(id, document.getSpdxId());
		assertEquals(name, document.getName());
		assertEquals(Constants.DEFAULT_SPDX_VERSION, document.getSpecVersion());

		// Verify creation date/time
		ZonedDateTime creationDateTime = document.getCreationTime();
		assertNotNull(creationDateTime);
		assertTrue(creationDateTime.isAfter(ZonedDateTime.now().minusHours(1)));

		// The data license should have no properties, just the URI;
		assertEquals("http://spdx.org/licenses/CC0-1.0", document.getPropertyAsResource(SpdxProperties.DATA_LICENSE).get().getURI());
		// Verify creation Info
		Resource creationInfo = document.getPropertyAsResource(SpdxProperties.CREATION_INFO).get();
		assertNotNull(creationInfo);
		StmtIterator creatorStatements = creationInfo.listProperties(SpdxProperties.CREATOR);
		Set<String> creatorStrings = stmtIteratorToStringSet(creatorStatements);

		Set<String> expectedCreators = ImmutableSet.of("Person: Albert Einstein (aEinstein@princeton.edu)", "Tool: SpdXtra",
				"Organization: Aperture Science, LLC. ()");
		assertEquals("Unexpected creator info", 0, Sets.symmetricDifference(expectedCreators, creatorStrings).size());

	}

	@Test
	public void testModifyCreationDate() {
		Dataset dataset = TestUtils.getDefaultDataSet();
		SpdxDocument document = Read.Document.get(dataset);
		ZonedDateTime newCreationDate = ZonedDateTime.of(1976, 7, 4, 9, 1, 2, 3, ZoneId.of("America/New_York"));
		// Expect the date to be stored in UTC, without nanoseconds.
		ZonedDateTime expectedCreationDate = ZonedDateTime.of(1976, 7, 4, 13, 1, 2, 0, ZoneId.of("UTC"));

		ModelUpdate update = Write.Document.updateCreationDate(document, newCreationDate);
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(update));

		document = Read.Document.get(dataset);
		assertEquals(expectedCreationDate, document.getCreationTime());

	}

	@Test
	public void testModifySpecVersion() {
		Dataset dataset = DatasetFactory.createTxnMem();
		final String baseUrl = "http://foo";
		final String spdxId = "SPDXRef-bar";
		final String name = "foobar";

		Write.applyUpdatesInOneTransaction(dataset, Write.New.document(baseUrl, spdxId, name, Creator.person("Myself", Optional.empty())),
				Write.Document.specVersion(baseUrl, spdxId, "666. Yep, you're doomed. DOOOOOMED!"));
		SpdxDocument document = Read.Document.get(dataset);
		assertEquals("666. Yep, you're doomed. DOOOOOMED!", document.getSpecVersion());
		

	}

	private static Set<String> stmtIteratorToStringSet(StmtIterator it) {
		Set<String> result = new HashSet<>();
		while (it.hasNext()) {
			result.add(it.next().getString());
		}
		return result;
	}

}
