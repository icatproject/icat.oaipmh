package org.icatproject.icat_oaipmh.integration;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.icatproject.icat_oaipmh.integration.BaseTest;
import org.icatproject.icat_oaipmh.integration.util.Setup;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TestVerbs extends BaseTest {

	@BeforeClass
	public static void setup() throws Exception {
		setup = new Setup("oaipmhInvStud.properties", "icatdump.data");
	}

	@Test
	public void testNoVerb() throws Exception {
		Document response = request("");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badVerb", errorCode.getTextContent());
	}

	@Test
	public void testUndefinedVerb() throws Exception {
		Document response = request("?verb=undefined");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badVerb", errorCode.getTextContent());
	}

	@Test
	public void testMultipleVerbs() throws Exception {
		Document response = request("?verb=ListMetadataFormats&verb=Identify");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testIdentify() throws Exception {
		Document response = request("?verb=Identify");

		getXmlNode(response, "Identify");

		Node repositoryName = getXmlNode(response, "repositoryName");
		assertEquals("Helmholtz-Zentrum Berlin für Materialien und Energie", repositoryName.getTextContent());

		NodeList adminEmail = getXmlNodes(response, "adminEmail", 2);
		assertEquals("someone@example.org", adminEmail.item(0).getTextContent());
		assertEquals("another@example.org", adminEmail.item(1).getTextContent());

		Node earliestDatestamp = getXmlNode(response, "earliestDatestamp");
		assertEquals("2018-07-24T10:24:06Z", earliestDatestamp.getTextContent());
	}

	@Test
	public void testListMetadataFormats() throws Exception {
		Document response = request("?verb=ListMetadataFormats");

		getXmlNode(response, "ListMetadataFormats");

		NodeList metadataFormat = getXmlNodes(response, "metadataFormat", 2);
		assertEquals("oai_dc", metadataFormat.item(0).getFirstChild().getTextContent());
		assertEquals("oai_datacite", metadataFormat.item(1).getFirstChild().getTextContent());
	}

	@Test
	public void testListMetadataFormatsStudy() throws Exception {
		Document response = null;

		// get unique identifier of last item in ListIdentifiers
		response = request("?verb=ListIdentifiers&metadataPrefix=oai_dc");
		Node listIdentifiers = getXmlNode(response, "ListIdentifiers");
		List<Node> headers = getXmlChildren(listIdentifiers, "header");
		Node studyHeader = headers.get(headers.size() - 1);
		String uniqueIdentifier = getXmlChild(studyHeader, "identifier").getTextContent();

		// get available metadata formats for that item (item should be a Study)
		response = request("?verb=ListMetadataFormats&identifier=" + uniqueIdentifier);
		getXmlNode(response, "ListMetadataFormats");

		Node metadataFormat = getXmlNode(response, "metadataFormat");
		assertEquals("oai_dc", metadataFormat.getFirstChild().getTextContent());
	}
}
