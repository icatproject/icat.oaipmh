package org.icatproject.icat_oaipmh;

import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.icatproject.icat_oaipmh.exceptions.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlResponse {

    private static final Logger logger = LoggerFactory.getLogger(XmlResponse.class);

    private DocumentBuilderFactory factory;
    private Document document;

    public XmlResponse() throws InternalException {
        factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.error(e.getMessage());
            throw new InternalException();
        }

        document = builder.newDocument();
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

    public Element addXmlInformation(XmlInformation info, String xmlElementName, Element anker) {
        Element xmlElement = null;
        if (anker == null)
            anker = document.getDocumentElement();
        if (xmlElementName == null)
            xmlElement = anker;
        else {
            xmlElement = document.createElement(xmlElementName);
            anker.appendChild(xmlElement);
        }

        for (Map.Entry<String, String> property : info.getSingleProperties().entrySet()) {
            Element el = document.createElement(property.getKey());
            el.appendChild(document.createTextNode(property.getValue()));
            xmlElement.appendChild(el);
        }

        for (Map.Entry<String, ? extends List<String>> property : info.getRepeatedProperties().entrySet()) {
            for (String value : property.getValue()) {
                Element el = document.createElement(property.getKey());
                el.appendChild(document.createTextNode(value));
                xmlElement.appendChild(el);
            }
        }

        for (Map.Entry<String, ? extends List<XmlInformation>> entry : info.getInformationLists().entrySet()) {
            for (XmlInformation information : entry.getValue()) {
                Element el = document.createElement(entry.getKey());
                addXmlInformation(information, null, el);
                xmlElement.appendChild(el);
            }
        }

        return xmlElement;
    }

    public Element addRecordInformation(List<RecordInformation> records, String xmlElementName,
            boolean includeMetadata) {
        Element xmlElement = document.createElement(xmlElementName);

        for (RecordInformation record : records) {
            Element recordElement = null;

            if (includeMetadata) {
                recordElement = document.createElement("record");
                addXmlInformation(record.getHeader(), "header", recordElement);
                addXmlInformation(record.getMetadata(), "metadata", recordElement);
            } else {
                recordElement = document.createElement("header");
                addXmlInformation(record.getHeader(), null, recordElement);
            }

            xmlElement.appendChild(recordElement);
        }

        document.getDocumentElement().appendChild(xmlElement);
        return xmlElement;
    }

    public void addResumptionToken(Element xmlElement, String resumptionToken) {
        Element el = document.createElement("resumptionToken");
        el.appendChild(document.createTextNode(resumptionToken));
        xmlElement.appendChild(el);
    }

    public String transformXml(Templates template) throws InternalException {
        Transformer transformer = null;
        Document doc = null;
        String output = null;

        try {
            if (template == null) {
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                transformer = transformerFactory.newTransformer();
                doc = document;
            } else {
                transformer = template.newTransformer();
                Source source = new DOMSource(document);
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.newDocument();
                Result result = new DOMResult(doc);
                transformer.transform(source, result);
            }

            DOMSource source = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            transformer.transform(source, new StreamResult(writer));
            output = writer.getBuffer().toString();
        } catch (TransformerException | ParserConfigurationException e) {
            logger.error(e.getMessage());
            throw new InternalException();
        }

        return output;
    }
}