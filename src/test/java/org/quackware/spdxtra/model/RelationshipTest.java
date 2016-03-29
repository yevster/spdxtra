package org.quackware.spdxtra.model;
import static org.junit.Assert.*;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.apache.jena.ext.com.google.common.collect.ImmutableList;
import org.apache.jena.ext.com.google.common.collect.Lists;
import org.apache.jena.query.Dataset;
import org.junit.Test;
import org.quackware.spdxtra.Read;
import org.quackware.spdxtra.RdfResourceUpdate;
import org.quackware.spdxtra.TestModelOperations;
import org.quackware.spdxtra.Write;
import org.quackware.spdxtra.model.Relationship.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationshipTest {
	private final static Logger logger = LoggerFactory.getLogger(RelationshipTest.class);
	
	@Test
	public void testUris(){
		assertEquals("http://spdx.org/rdf/terms#relationshipType_describes", Type.DESCRIBES.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_generates", Type.GENERATES.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_testcaseOf", Type.TESTCASE_OF.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_containedBy", Type.CONTAINED_BY.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_dataFile", Type.DATA_FILE.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_buildToolOf", Type.BUILD_TOOL_OF.getUri());
		
	}
	
	@Test
	public void testFromUriOrLocalName(){
		assertEquals(Type.DESCRIBES, Type.fromUri("http://spdx.org/rdf/terms#relationshipType_describes"));
		assertEquals(Type.DESCRIBES, Type.fromUri("relationshipType_describes"));
		assertEquals(Type.DESCRIBES, Type.fromUri("describes"));
		
		assertEquals(Type.CONTAINED_BY, Type.fromUri("http://spdx.org/rdf/terms#relationshipType_containedBy"));
		assertEquals(Type.CONTAINED_BY, Type.fromUri("relationshipType_containedBy"));
		assertEquals(Type.CONTAINED_BY, Type.fromUri("containedBy"));
		
		assertEquals(Type.DATA_FILE, Type.fromUri("http://spdx.org/rdf/terms#relationshipType_dataFile"));
		assertEquals(Type.DATA_FILE, Type.fromUri("relationshipType_dataFile"));
		assertEquals(Type.DATA_FILE, Type.fromUri("dataFile"));
	}
	
	@Test
	public void testAddRelationship(){
		Dataset dataset = TestModelOperations.getDefaultDataSet();
		SpdxPackage pkg = new SpdxPackage(Read.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-1").get());
		
		//We start with a file with two relationships
		SpdxFile file = new SpdxFile(Read.lookupResourceByUri(dataset, "http://spdx.org/documents/spdx-toolsv2.0-rc1#SPDXRef-164").get());
		List<Relationship> originalRelationships = Lists.newLinkedList(Read.getRelationships(dataset, file));
		assertEquals(1, originalRelationships.size());
		//Now, let's add a new perposterous relationship.
		String comment = "This is a garbage relationship";
		RdfResourceUpdate sillyUpdate = Relationship.addRelationship(file, pkg, Optional.of(comment), Relationship.Type.EXPANDED_FROM_ARCHIVE);
		Write.applyUpdatesInOneTransaction(dataset, ImmutableList.of(sillyUpdate));
		
		//1 relationships + 1 new relationship = 2 relationships!
		List<Relationship> newRelationships = Lists.newLinkedList(Read.getRelationships(dataset, file));
		assertEquals(2, newRelationships.size());
		//Let's grab the newbie!
		newRelationships.removeAll(originalRelationships);
		assertEquals(1, newRelationships.size());
		Relationship newRelationship = newRelationships.get(0);
		assertEquals(Relationship.Type.EXPANDED_FROM_ARCHIVE, newRelationship.getType());
		assertEquals(comment, newRelationship.getComment());
		assertEquals(pkg, newRelationship.getRelatedElement());

		
	}
}
