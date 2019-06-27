package org.icatproject.icat_oaipmh;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestHelloWorld {
	@Test
	public void test() throws Exception {
		RequestInterface i = new RequestInterface();
		assertEquals("{\"version\":\"1.0.0-SNAPSHOT\"}", i.getVersion());
	}
}