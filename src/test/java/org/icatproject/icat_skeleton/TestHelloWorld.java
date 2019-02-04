package org.icatproject.icat_skeleton;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestHelloWorld {
	@Test
	public void test() throws Exception {
		SimpleREST a = new SimpleREST();
		assertEquals("{\"Hello\":\"World\"}", a.hello());
	}
}