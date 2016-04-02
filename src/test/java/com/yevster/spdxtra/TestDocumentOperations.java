package com.yevster.spdxtra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.yevster.spdxtra.Read;
import com.yevster.spdxtra.Write;
import com.yevster.spdxtra.Write.ModelUpdate;
import com.yevster.spdxtra.model.SpdxDocument;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.junit.Test;

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
