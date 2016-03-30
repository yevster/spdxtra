package org.quackware.spdxtra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.jena.ext.com.google.common.collect.ImmutableList;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.junit.Test;
import org.quackware.spdxtra.Write.ModelUpdate;
import org.quackware.spdxtra.model.SpdxDocument;

public class TestDocumentOperations {

	@Test
	public void createDocument() {
		final String id = "SPDXRef-Wickendess";
		final String baseUrl = "http://example.org";
		final String name = "Que paso?";
		Dataset dataset = DatasetFactory.createTxnMem();

		ModelUpdate creation = Write.New.document(baseUrl, id, name);
		assertNotNull(creation);
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(creation));
		SpdxDocument document = Read.Document.get(dataset);
		assertNotNull(document);
		assertEquals(baseUrl + "#" + id, document.getUri());
		assertEquals(id, document.getSpdxId());
		assertEquals(name, document.getName());
	
	}
}
