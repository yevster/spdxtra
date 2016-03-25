package org.quackware.spdxtra.model;
import static org.junit.Assert.*;

import org.junit.Test;

public class RelationshipTest {
	@Test
	public void testUris(){
		assertEquals("http://spdx.org/rdf/terms#relationshipType_describes", Relationship.DESCRIBES.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_generates", Relationship.GENERATES.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_testcaseOf", Relationship.TESTCASE_OF.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_containedBy", Relationship.CONTAINED_BY.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_dataFile", Relationship.DATA_FILE.getUri());
		assertEquals("http://spdx.org/rdf/terms#relationshipType_buildToolOf", Relationship.BUILD_TOOL_OF.getUri());
		
	}
}
