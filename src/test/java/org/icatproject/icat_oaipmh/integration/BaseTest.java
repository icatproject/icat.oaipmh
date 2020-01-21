package org.icatproject.icat_oaipmh.integration;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.icatproject.icat_oaipmh.integration.util.Setup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class BaseTest {

	protected static Setup setup;

	public static Document request(String urlParameters)
			throws IOException, ParserConfigurationException, SAXException {
		String url = setup.getRequestUrl() + urlParameters;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(url);

		return document;
	}

	public static NodeList getXmlNodes(Document doc, String nodeName, int expectedNumber) {
		NodeList nodes = doc.getElementsByTagName(nodeName);
		assertEquals(expectedNumber, nodes.getLength());
		return nodes;
	}

	public static Node getXmlNode(Document doc, String nodeName) {
		NodeList nodes = doc.getElementsByTagName(nodeName);
		assertEquals(1, nodes.getLength());
		return nodes.item(0);
	}

	public static List<Node> getXmlChildren(Node parent, String name) {
		List<Node> nl = new ArrayList<Node>();
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && name.equals(child.getNodeName())) {
				nl.add(child);
			}
		}
		return nl;
	}

	public static Node getXmlChild(Node parent, String name) {
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element && name.equals(child.getNodeName())) {
				return child;
			}
		}
		return null;
	}
}
