package org.icatproject.icat_oaipmh.integration;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

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
	public void testResponseOutline() throws Exception {
		Document response = request(
				"?verb=ListIdentifiers&metadataPrefix=oai_dc&from=2018-01-01T00:00:00Z&until=2018-12-31T00:00:00Z");

		getXmlNode(response, "ListIdentifiers");
		getXmlNode(response, "responseDate");
		Node request = getXmlNode(response, "request");

		NamedNodeMap attributes = request.getAttributes();
		Node verb = attributes.getNamedItem("verb");
		Node metadataPrefix = attributes.getNamedItem("metadataPrefix");
		Node from = attributes.getNamedItem("from");
		Node until = attributes.getNamedItem("until");
		assertEquals("ListIdentifiers", verb.getTextContent());
		assertEquals("oai_dc", metadataPrefix.getTextContent());
		assertEquals("2018-01-01T00:00:00Z", from.getTextContent());
		assertEquals("2018-12-31T00:00:00Z", until.getTextContent());
		assertEquals(setup.getRequestUrl(), request.getTextContent());
	}

	@Test
	public void testMissingVerb() throws Exception {
		Document response = request("");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badVerb", errorCode.getTextContent());
	}

	@Test
	public void testNoVerb() throws Exception {
		Document response = request("?verb");

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
		assertEquals("badVerb", errorCode.getTextContent());
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
		assertEquals("2018-07-13T15:34:36Z", earliestDatestamp.getTextContent());
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
		assertEquals("oai_dc", getXmlChild(metadataFormat.item(0), "metadataPrefix").getTextContent());
		assertEquals("oai_datacite", getXmlChild(metadataFormat.item(1), "metadataPrefix").getTextContent());
	}

	@Test
	public void testListMetadataFormatsStudy() throws Exception {
		Document response = request("?verb=ListMetadataFormats&identifier=" + studyUniqueIdentifier);

		getXmlNode(response, "ListMetadataFormats");

		Node metadataFormat = getXmlNode(response, "metadataFormat");
		assertEquals("oai_dc", getXmlChild(metadataFormat, "metadataPrefix").getTextContent());
	}

	@Test
	public void testListMetadataFormatsInvalidIdentifier() throws Exception {
		Document response = request("?verb=ListMetadataFormats&identifier=oai:invalid:stud/0");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("idDoesNotExist", errorCode.getTextContent());
	}

	@Test
	public void testListSets() throws Exception {
		Document response = request("?verb=ListSets");

		Node listSets = getXmlNode(response, "ListSets");
		List<Node> sets = getXmlChildren(listSets, "set");

		assertEquals(3, sets.size());

		for (Node set : sets) {
			String setSpec = getXmlChild(set, "setSpec").getTextContent();
			String setName = getXmlChild(set, "setName").getTextContent();

			if (setSpec.equals("exampleSetA"))
				assertEquals("Example Set A", setName);
			else if (setSpec.equals("exampleSetB"))
				assertEquals("Example Set B", setName);
			else if (setSpec.equals("exampleSetC"))
				assertEquals("Example Set C", setName);
			else
				throw new AssertionError("Unknown setSpec: " + setSpec);
		}
	}

	@Test
	public void testGetRecordMissingIdentifier() throws Exception {
		Document response = request("?verb=GetRecord&metadataPrefix=oai_datacite");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testGetRecordInvalidIdentifier() throws Exception {
		Document response = request("?verb=GetRecord&metadataPrefix=oai_datacite&identifier=oai:invalid:stud/0");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("idDoesNotExist", errorCode.getTextContent());
	}

	@Test
	public void testGetRecordMissingMetadataFormat() throws Exception {
		Document response = request("?verb=GetRecord&identifier=" + investigationUniqueIdentifier);

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testGetRecordInvalidMetadataFormat() throws Exception {
		Document response = request(
				"?verb=GetRecord&metadataPrefix=invalid&identifier=" + investigationUniqueIdentifier);

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("cannotDisseminateFormat", errorCode.getTextContent());
	}

	@Test
	public void testGetRecordUnsupportedMetadataFormat() throws Exception {
		Document response = request("?verb=GetRecord&metadataPrefix=oai_datacite&identifier=" + studyUniqueIdentifier);

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("cannotDisseminateFormat", errorCode.getTextContent());
	}

	@Test
	public void testGetRecordInvestigation() throws Exception {
		Document response = request(
				"?verb=GetRecord&metadataPrefix=oai_datacite&identifier=" + investigationUniqueIdentifier);

		getXmlNode(response, "GetRecord");

		Node header = getXmlNode(response, "header");
		String identifier = getXmlChild(header, "identifier").getTextContent();
		assertEquals(investigationUniqueIdentifier, identifier);
		String setSpec = getXmlChild(header, "setSpec").getTextContent();
		assertEquals("exampleSetC", setSpec);

		getXmlNodes(response, "dataConfigurationIdentifier", 0);
	}

	@Test
	public void testGetRecordStudy() throws Exception {
		Document response = request("?verb=GetRecord&metadataPrefix=oai_dc&identifier=" + studyUniqueIdentifier);

		getXmlNode(response, "GetRecord");

		Node header = getXmlNode(response, "header");
		String identifier = getXmlChild(header, "identifier").getTextContent();
		assertEquals(studyUniqueIdentifier, identifier);
		String setSpec = getXmlChild(header, "setSpec").getTextContent();
		assertEquals("exampleSetB", setSpec);

		getXmlNodes(response, "dataConfigurationIdentifier", 0);
	}

	@Test
	public void testListIdentifiersMissingMetadataFormat() throws Exception {
		Document response = request("?verb=ListIdentifiers");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testListIdentifiersInvalidMetadataFormat() throws Exception {
		Document response = request("?verb=ListIdentifiers&metadataPrefix=invalid");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("cannotDisseminateFormat", errorCode.getTextContent());
	}

	@Test
	public void testListIdentifiersIncompleteResumptionToken() throws Exception {
		Document response = request("?verb=ListIdentifiers&resumptionToken=oai_dc");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badResumptionToken", errorCode.getTextContent());
	}

	@Test
	public void testListIdentifiersResumptionTokenAndAdditionalArguments() throws Exception {
		Document response = request(
				"?verb=ListIdentifiers&metadataPrefix=oai_dc&resumptionToken=oai_dc,inv/0,null,null,null");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testListIdentifiersInvalidResumptionTokenMetadataFormat() throws Exception {
		Document response = request("?verb=ListIdentifiers&resumptionToken=invalid,inv/0,null,null,null");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("cannotDisseminateFormat", errorCode.getTextContent());
	}

	@Test
	public void testListIdentifiersNoRecords() throws Exception {
		Document response = request("?verb=ListIdentifiers&metadataPrefix=oai_dc&until=1900-01-01T00:00:00Z");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("noRecordsMatch", errorCode.getTextContent());
	}

	@Test
	public void testListIdentifiersInvalidTimespan() throws Exception {
		Document response = request(
				"?verb=ListIdentifiers&metadataPrefix=oai_dc&from=2018-09-01T00:00:00Z&until=2018-07-01T00:00:00Z");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testListIdentifiers() throws Exception {
		Document response = request("?verb=ListIdentifiers&metadataPrefix=oai_dc");

		getXmlNode(response, "ListIdentifiers");
		getXmlNodes(response, "header", 2);
		getXmlNode(response, "resumptionToken");
	}

	@Test
	public void testListIdentifiersWithResumptionTokenAndSet() throws Exception {
		Document response;
		String resumptionToken;

		response = request("?verb=ListIdentifiers&metadataPrefix=oai_dc&set=exampleSetB");

		getXmlNode(response, "ListIdentifiers");
		getXmlNodes(response, "header", 2);
		resumptionToken = getXmlNode(response, "resumptionToken").getTextContent();

		response = request("?verb=ListIdentifiers&resumptionToken=" + resumptionToken);

		getXmlNode(response, "ListIdentifiers");
		getXmlNodes(response, "header", 1);
		resumptionToken = getXmlNode(response, "resumptionToken").getTextContent();

		assertEquals("", resumptionToken);
	}

	@Test
	public void testListIdentifiersWithResumptionTokenAndTimespan() throws Exception {
		Document response = request(
				"?verb=ListIdentifiers&resumptionToken=oai_dc,inv/0,2018-07-01T00:00:00Z,2018-07-15T00:00:00Z,null");

		getXmlNode(response, "ListIdentifiers");
		getXmlNodes(response, "header", 1);
	}

	@Test
	public void testListIdentifiersWithTimespan() throws Exception {
		Document response = request(
				"?verb=ListIdentifiers&metadataPrefix=oai_dc&from=2018-07-01T00:00:00Z&until=2018-07-15T00:00:00Z");

		getXmlNode(response, "ListIdentifiers");
		getXmlNodes(response, "header", 1);
	}

	@Test
	public void testListIdentifiersWithDatespan() throws Exception {
		Document response = request("?verb=ListIdentifiers&metadataPrefix=oai_dc&from=2018-07-01&until=2018-07-15");

		getXmlNode(response, "ListIdentifiers");
		getXmlNodes(response, "header", 1);
	}

	@Test
	public void testListIdentifiersWithDateAndTimespan() throws Exception {
		Document response = request(
				"?verb=ListIdentifiers&metadataPrefix=oai_dc&from=2018-07-01&until=2018-07-15T00:00:00Z");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testListIdentifiersWithSet() throws Exception {
		Document response = request("?verb=ListIdentifiers&metadataPrefix=oai_dc&set=exampleSetA");

		getXmlNode(response, "ListIdentifiers");
		getXmlNodes(response, "header", 2);

		String resumptionToken = getXmlNode(response, "resumptionToken").getTextContent();
		assertEquals("", resumptionToken);
	}

	@Test
	public void testListIdentifiersWithUndefinedSet() throws Exception {
		Document response = request("?verb=ListIdentifiers&metadataPrefix=oai_dc&set=undefined");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("noRecordsMatch", errorCode.getTextContent());
	}

	@Test
	public void testListRecordsMissingMetadataFormat() throws Exception {
		Document response = request("?verb=ListRecords");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testListRecordsInvalidMetadataFormat() throws Exception {
		Document response = request("?verb=ListRecords&metadataPrefix=invalid");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("cannotDisseminateFormat", errorCode.getTextContent());
	}

	@Test
	public void testListRecordsIncompleteResumptionToken() throws Exception {
		Document response = request("?verb=ListRecords&resumptionToken=oai_dc");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badResumptionToken", errorCode.getTextContent());
	}

	@Test
	public void testListRecordsResumptionTokenAndAdditionalArguments() throws Exception {
		Document response = request(
				"?verb=ListRecords&metadataPrefix=oai_dc&resumptionToken=oai_dc,inv/0,null,null,null");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testListRecordsInvalidResumptionTokenMetadataFormat() throws Exception {
		Document response = request("?verb=ListRecords&resumptionToken=invalid,inv/0,null,null,null");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("cannotDisseminateFormat", errorCode.getTextContent());
	}

	@Test
	public void testListRecordsNoRecords() throws Exception {
		Document response = request("?verb=ListRecords&metadataPrefix=oai_dc&until=1900-01-01T00:00:00Z");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("noRecordsMatch", errorCode.getTextContent());
	}

	@Test
	public void testListRecordsInvalidTimespan() throws Exception {
		Document response = request(
				"?verb=ListRecords&metadataPrefix=oai_dc&from=2018-09-01T00:00:00Z&until=2018-07-01T00:00:00Z");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testListRecords() throws Exception {
		Document response = request("?verb=ListRecords&metadataPrefix=oai_dc");

		getXmlNode(response, "ListRecords");
		getXmlNodes(response, "record", 2);
		getXmlNodes(response, "dataConfigurationIdentifier", 0);
		getXmlNode(response, "resumptionToken");
	}

	@Test
	public void testListRecordsWithResumptionTokenAndSet() throws Exception {
		Document response;
		String resumptionToken;

		response = request("?verb=ListRecords&metadataPrefix=oai_dc&set=exampleSetB");

		getXmlNode(response, "ListRecords");
		getXmlNodes(response, "record", 2);
		resumptionToken = getXmlNode(response, "resumptionToken").getTextContent();

		response = request("?verb=ListRecords&resumptionToken=" + resumptionToken);

		getXmlNode(response, "ListRecords");
		getXmlNodes(response, "record", 1);
		resumptionToken = getXmlNode(response, "resumptionToken").getTextContent();

		assertEquals("", resumptionToken);
	}

	@Test
	public void testListRecordsWithResumptionTokenAndTimespan() throws Exception {
		Document response = request(
				"?verb=ListRecords&resumptionToken=oai_dc,inv/0,2018-07-01T00:00:00Z,2018-07-15T00:00:00Z,null");

		getXmlNode(response, "ListRecords");
		getXmlNodes(response, "record", 1);
	}

	@Test
	public void testListRecordsWithTimespan() throws Exception {
		Document response = request(
				"?verb=ListRecords&metadataPrefix=oai_dc&from=2018-07-01T00:00:00Z&until=2018-07-15T00:00:00Z");

		getXmlNode(response, "ListRecords");
		getXmlNodes(response, "record", 1);
	}

	@Test
	public void testListRecordsWithDatespan() throws Exception {
		Document response = request("?verb=ListRecords&metadataPrefix=oai_dc&from=2018-07-01&until=2018-07-15");

		getXmlNode(response, "ListRecords");
		getXmlNodes(response, "record", 1);
	}

	@Test
	public void testListRecordsWithDateAndTimespan() throws Exception {
		Document response = request(
				"?verb=ListRecords&metadataPrefix=oai_dc&from=2018-07-01&until=2018-07-15T00:00:00Z");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("badArgument", errorCode.getTextContent());
	}

	@Test
	public void testListRecordsWithSet() throws Exception {
		Document response = request("?verb=ListRecords&metadataPrefix=oai_dc&set=exampleSetA");

		getXmlNode(response, "ListRecords");
		getXmlNodes(response, "record", 2);

		String resumptionToken = getXmlNode(response, "resumptionToken").getTextContent();
		assertEquals("", resumptionToken);
	}

	@Test
	public void testListRecordsWithUndefinedSet() throws Exception {
		Document response = request("?verb=ListRecords&metadataPrefix=oai_dc&set=undefined");

		Node error = getXmlNode(response, "error");
		NamedNodeMap attributes = error.getAttributes();
		Node errorCode = attributes.getNamedItem("code");
		assertEquals("noRecordsMatch", errorCode.getTextContent());
	}
}
