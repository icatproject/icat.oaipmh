package org.icatproject.icat_oai;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XmlResponse {

    private static final Logger logger = LoggerFactory.getLogger(XmlResponse.class);

    private DocumentBuilderFactory factory;
    private Document document;

    public XmlResponse() {
        factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.error(e.getMessage());
        }

        document = builder.newDocument();
    }

    public Document getDocument() {
        return document;
    }

    public void makeResponseOutline(String requestUrl, Map<String, String> params) {

        Element rootElement = document.createElement("OAI-PMH");
        rootElement.setAttribute("xmlns", "http://www.openarchives.org/OAI/2.0/");
        rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        rootElement.setAttribute("xsi:schemaLocation",
                "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd");
        document.appendChild(rootElement);

        String strDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        Element responseDate = document.createElement("responseDate");
        responseDate.appendChild(document.createTextNode(strDate));
        rootElement.appendChild(responseDate);

        Element request = document.createElement("request");
        for (Map.Entry<String, String> param : params.entrySet()) {
            request.setAttribute(param.getKey(), param.getValue());
        }
        request.appendChild(document.createTextNode(requestUrl));
        rootElement.appendChild(request);
    }

    public void addError(String errorCode, String errorMessage) {
        Element error = document.createElement("error");
        error.setAttribute("code", errorCode);
        error.appendChild(document.createTextNode(errorMessage));
        document.getDocumentElement().appendChild(error);
    }

    public void addContent(Node content) {
        document.getDocumentElement().appendChild(content);
    }
}