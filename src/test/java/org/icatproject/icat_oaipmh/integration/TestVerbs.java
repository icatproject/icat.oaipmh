package org.icatproject.icat_oaipmh.integration;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
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

	private static String investigationUniqueIdentifier;
	private static String studyUniqueIdentifier;

	@BeforeClass
	public static void setup() throws Exception {
		setup = new Setup("oaipmhInvStud.properties", "icatdump.data");
		fetchExampleIdentifiers();
	}

	public static void fetchExampleIdentifiers() throws Exception {
		List<String> identifiers = new ArrayList<String>();

		Document response = request("?verb=ListIdentifiers&metadataPrefix=oai_dc");
		String resumptionToken = null;

		do {
			Node listIdentifiers = getXmlNode(response, "ListIdentifiers");
			List<Node> headers = getXmlChildren(listIdentifiers, "header");
			for (Node header : headers) {
				identifiers.add(getXmlChild(header, "identifier").getTextContent());
			}
			resumptionToken = getXmlChild(listIdentifiers, "resumptionToken").getTextContent();
			response = request("?verb=ListIdentifiers&resumptionToken=" + resumptionToken);
		} while (!resumptionToken.equals(""));

		// Investigation identifier is first item in the list
		investigationUniqueIdentifier = identifiers.get(0);
		// Study identifier is last item in the list
		studyUniqueIdentifier = identifiers.get(identifiers.size() - 1);
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
	public void testIdentifyWithArgument() throws Exception {
		Document response = request("?verb=Identify&metadataPrefix=oai_dc");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
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

		response = request("?verb=ListMetadataFormats&identifier=" + studyUniqueIdentifier);
		getXmlNode(response, "ListMetadataFormats");

		Node metadataFormat = getXmlNode(response, "metadataFormat");
		assertEquals("oai_dc", metadataFormat.getFirstChild().getTextContent());
	}

	@Test
	public void testListMetadataFormatsInvalidIdentifier() throws Exception {
		Document response = request("?verb=ListMetadataFormats&identifier=invalid");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("idDoesNotExist", errorCode.getTextContent());
	}
}
