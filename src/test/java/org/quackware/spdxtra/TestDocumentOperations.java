package org.quackware.spdxtra;

import org.apache.jena.ext.com.google.common.collect.ImmutableList;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.junit.Test;
import org.quackware.spdxtra.model.SpdxDocument;

import static org.junit.Assert.*;

public class TestDocumentOperations {
	@Test
	public void createDocument() {
		final String id = "SPDXRef-Wickendess";
		final String baseUrl = "http://example.org";
		Dataset dataset = DatasetFactory.createTxnMem();
		RdfResourceUpdate creation = Write.New.document(baseUrl, id);
		assertNotNull(creation);
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(creation));
		SpdxDocument document = Read.Document.get(dataset);
		assertNotNull(document);
		assertEquals(baseUrl+"#"+id, document.getSpdxId());
		
	}
}
