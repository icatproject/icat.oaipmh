package org.icatproject.icat_component;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestGetDescription {
	@Test
	public void test() throws Exception {
		SimpleREST a = new SimpleREST();
		assertEquals("{\"keys\":[{\"name\":\"username\"},{\"name\":\"password\",\"hide\":true}]}", a.getDescription());
	}
}