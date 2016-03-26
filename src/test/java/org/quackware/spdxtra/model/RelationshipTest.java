package org.quackware.spdxtra.model;
import static org.junit.Assert.*;

import org.junit.Test;
import org.quackware.spdxtra.model.Relationship.Type;

public class RelationshipTest {
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
}
