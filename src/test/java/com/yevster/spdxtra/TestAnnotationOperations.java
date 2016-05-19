package com.yevster.spdxtra;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.Assert;
import org.junit.Test;

import com.yevster.spdxtra.Write.ModelUpdate;
import com.yevster.spdxtra.model.Annotation;
import com.yevster.spdxtra.model.Creator;

public class TestAnnotationOperations {
	@Test
	public void testAddingAnnotation() {
		Dataset dataset = DatasetFactory.createTxnMem();
		final String baseUrl = "http://example.com";
		final String documentSpdxId = "SPDXRef-MyDoc";
		final String packageSpdxId = "SPDXRef-MyBundleOfJoy";
		final String packageName = "BundleOfJoy";
		final String packageUri = baseUrl + "#" + packageSpdxId;

		List<ModelUpdate> updates = new LinkedList<>();
		updates.add(Write.New.document(baseUrl, documentSpdxId, "El documento fantastico!", Creator.tool("Testy McTestface")));
		updates.add(Write.Document.addPackage(baseUrl, documentSpdxId, packageSpdxId, packageName));
		Write.applyUpdatesInOneTransaction(dataset, updates);

		// No annotations by default, right?
		Resource pkgResource = dataset.getDefaultModel().getResource(packageUri);
		Assert.assertFalse(pkgResource.listProperties(SpdxProperties.ANNOTATION).hasNext());

		// Let's add an annotation
		ZonedDateTime annotationTime = ZonedDateTime.of(1776, 7, 4, 7, 4, 22, 0, ZoneId.of("America/New_York"));
		Creator annotator = Creator.person("Borat Sagdiev", Optional.of("bdsagdiev@moviefilmstudio.kz"));
		String comment = "Very nice! I like!";
		Annotation.Type type = Annotation.Type.REVIEW;

		Write.applyUpdatesInOneTransaction(dataset,
				Write.New.annotation(baseUrl, packageSpdxId, type, annotationTime, annotator, comment));
		// Test the annotation
		pkgResource = dataset.getDefaultModel().getResource(packageUri);
		Iterator<Statement> stIt = pkgResource.listProperties(SpdxProperties.ANNOTATION);
		Assert.assertTrue(stIt.hasNext());
		Annotation annotation = new Annotation(stIt.next().getObject().asResource());
		Assert.assertEquals(SpdxUris.SPDX_TERMS + "Annotation",
				annotation.rdfResource.getProperty(SpdxProperties.RDF_TYPE).getObject().asResource().getURI());
		Assert.assertFalse("Added only one annotation, but found multiple.", stIt.hasNext());
		Assert.assertEquals(comment, annotation.getComment().get());
		Assert.assertEquals(ZonedDateTime.ofInstant(annotationTime.toInstant(), ZoneId.of("UTC")), annotation.getDate());
		Assert.assertEquals(annotator, annotation.getAnnotator());
		Assert.assertEquals(type, annotation.getType());

	}
}
