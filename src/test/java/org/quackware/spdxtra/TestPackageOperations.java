package org.quackware.spdxtra;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.ext.com.google.common.collect.Iterables;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.Dataset;
import org.junit.Test;
import org.quackware.spdxtra.model.Relationship;
import org.quackware.spdxtra.model.SpdxDocument;
import org.quackware.spdxtra.model.SpdxPackage;

public class TestPackageOperations {

	@Test
	public void testPackageRead() throws IOException {
		Dataset dataset = TestModelOperations.getDefaultDataSet();
		Iterable<SpdxPackage> packages = ModelDataAccess.getAllPackages(dataset);
		SpdxPackage pkg = Iterables.find(packages, (p) -> "SPDXRef-1".equals(p.getSpdxId()));

		assertEquals(NoneNoAssertionOrValue.NO_ASSERTION, pkg.getCopyright());
		assertEquals("SPDX tools", pkg.getName());
		assertEquals("2.0.0-RC1", pkg.getVersionInfo().orElse("NO VERSION? AWWWWWW..."));
		assertEquals(12, Iterables.size(ModelDataAccess.getFilesForPackage(pkg)));
	}

	@Test
	public void testPackageFieldUpdates() {
		Dataset dataset = TestModelOperations.getDefaultDataSet();
		SpdxDocument doc = ModelDataAccess.getDocument(dataset);
		List<Relationship> relationships = Lists.newLinkedList(ModelDataAccess.getRelationships(dataset, doc, Relationship.Type.DESCRIBES));
		assertEquals(1, relationships.size());
		assertEquals(SpdxPackage.class, relationships.get(0).getRelatedElement().getClass());
		SpdxPackage pkg = (SpdxPackage) relationships.get(0).getRelatedElement();

		List<RdfResourceUpdate> updates = new LinkedList<>();
		// Let's update the name
		final String newName = "Definitely not the old name";
		final String oldName = pkg.getName();
		assertEquals("SPDX tools", oldName);
		RdfResourceUpdate update = SpdxPackage.updatePackageName(pkg, newName);
		// The package should not have been changed.
		assertEquals(oldName, pkg.getName());
		assertEquals(pkg.getUri(), update.getResourceUri());
		updates.add(update);
		
		// Apply the updates
		ModelDataAccess.applyUpdatesInOneTransaction(dataset, updates);

		//Reload the package from the document model
		pkg = (SpdxPackage) ModelDataAccess.getRelationships(dataset, doc, Relationship.Type.DESCRIBES).iterator().next()
				.getRelatedElement();
		assertEquals(newName, pkg.getName());

	}

}
