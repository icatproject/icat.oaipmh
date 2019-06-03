package org.icatproject.icat_oai;

import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.servlet.http.HttpServletRequest;

import org.icatproject.icat.client.ICAT;
import org.icatproject.icat.client.IcatException;
import org.icatproject.icat.client.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ResponseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResponseBuilder.class);

    private Session icatSession;
    private final String repositoryName;
    private final String[] adminEmails;
    private ArrayList<MetadataFormat> metadataFormats;

    public ResponseBuilder(String repositoryName, String[] adminEmails) {
        metadataFormats = new ArrayList<MetadataFormat>();
        this.repositoryName = repositoryName;
        this.adminEmails = adminEmails;
    }

    public void performIcatLogin(String icatUrl, String[] icatAuth) {
        ICAT restIcat = null;
        try {
            restIcat = new ICAT(icatUrl);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage());
        }

        HashMap<String, String> credentials = new HashMap<String, String>();
        credentials.put(icatAuth[1], icatAuth[2]);
        credentials.put(icatAuth[3], icatAuth[4]);

        try {
            icatSession = restIcat.login(icatAuth[0], credentials);
        } catch (IcatException e) {
            logger.error(e.getMessage());
        }
    }

    public void addMetadataFormat(MetadataFormat format) {
        metadataFormats.add(format);
    }

    public void buildIdentifyResponse(HttpServletRequest req, XmlResponse res) {
        Document doc = res.getDocument();

        Element identify = doc.createElement("Identify");

        Element repo = doc.createElement("repositoryName");
        repo.appendChild(doc.createTextNode(repositoryName));
        identify.appendChild(repo);

        Element base = doc.createElement("baseURL");
        base.appendChild(doc.createTextNode(getRequestUrl(req)));
        identify.appendChild(base);

        Element protocol = doc.createElement("protocolVersion");
        protocol.appendChild(doc.createTextNode("2.0"));
        identify.appendChild(protocol);

        for (String adminEmail : adminEmails) {
            Element email = doc.createElement("adminEmail");
            email.appendChild(doc.createTextNode(adminEmail));
            identify.appendChild(email);
        }

        String earliestDatestamp = "";
        try {
            String result = icatSession.search("SELECT inv.modTime FROM Investigation inv ORDER BY inv.modTime");
            JsonReader jsonReader = Json.createReader(new java.io.StringReader(result));
            JsonArray jsonArray = jsonReader.readArray();
            jsonReader.close();
            JsonString earliestDate = jsonArray.getJsonString(0);
            earliestDatestamp = getFormattedDateTime(earliestDate.getString());
        } catch (IcatException e) {
            logger.error(e.getMessage());
        }
        Element earliest = doc.createElement("earliestDatestamp");
        earliest.appendChild(doc.createTextNode(earliestDatestamp));
        identify.appendChild(earliest);

        Element deleted = doc.createElement("deletedRecord");
        deleted.appendChild(doc.createTextNode("transient"));
        identify.appendChild(deleted);

        Element granularity = doc.createElement("granularity");
        granularity.appendChild(doc.createTextNode("YYYY-MM-DDThh:mm:ssZ"));
        identify.appendChild(granularity);

        res.addContent(identify);
    }

    public void buildListIdentifiersResponse(HttpServletRequest req, XmlResponse res) {
        Document doc = res.getDocument();

        Element listIdentifiers = doc.createElement("ListIdentifiers");
        getIcatHeaders(req, res, listIdentifiers);

        res.addContent(listIdentifiers);
    }

    public void buildListSetsResponse(HttpServletRequest req, XmlResponse res) {
        res.addError("noSetHierarchy", "This repository does not support sets");
    }

    public void buildListMetadataFormatsResponse(HttpServletRequest req, XmlResponse res) {
        Document doc = res.getDocument();

        Element listMetadataFormats = doc.createElement("ListMetadataFormats");

        for (MetadataFormat format : metadataFormats) {
            Element metadataFormat = doc.createElement("metadataFormat");

            Element metadataPrefix = doc.createElement("metadataPrefix");
            metadataPrefix.appendChild(doc.createTextNode(format.getMetadataPrefix()));
            metadataFormat.appendChild(metadataPrefix);

            Element metadataNamespace = doc.createElement("metadataNamespace");
            metadataNamespace.appendChild(doc.createTextNode(format.getMetadataNamespace()));
            metadataFormat.appendChild(metadataNamespace);

            Element schema = doc.createElement("schema");
            schema.appendChild(doc.createTextNode(format.getMetadataSchema()));
            metadataFormat.appendChild(schema);

            listMetadataFormats.appendChild(metadataFormat);
        }

        res.addContent(listMetadataFormats);
    }

    public void getIcatHeaders(HttpServletRequest req, XmlResponse res, Element el) {
        try {
            String result = icatSession.search("SELECT inv.id, inv.modTime FROM Investigation inv ORDER BY inv.modTime");
            JsonReader jsonReader = Json.createReader(new java.io.StringReader(result));
            JsonArray jsonArray = jsonReader.readArray();
            jsonReader.close();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonArray item = jsonArray.getJsonArray(i);
                String identifier = getFormattedIdentifier(req.getServerName(), item.getJsonNumber(0).toString());
                String datestamp = getFormattedDateTime(item.getJsonString(1).getString());
                appendXmlHeader(el, identifier, datestamp);
            }
        } catch (IcatException e) {
            logger.error(e.getMessage());
        }
    }

    public void appendXmlHeader(Element el, String identifier, String datestamp) {
        Document doc = el.getOwnerDocument();
        Element header = doc.createElement("header");

        Element id = doc.createElement("identifier");
        id.setTextContent(identifier);

        Element date = doc.createElement("datestamp");
        date.setTextContent(datestamp);

        header.appendChild(id);
        header.appendChild(date);

        el.appendChild(header);
    }

    public String getFormattedIdentifier(String url, String id) {
        return "oai:" + url.toString() + ":" + id.toString();
    }

    public String getFormattedDateTime(String dateTime) {
        return OffsetDateTime.parse(dateTime).format(DateTimeFormatter.ISO_INSTANT);
    }

    public String getRequestUrl(HttpServletRequest req) {
        String scheme = req.getScheme();
        String serverName = req.getServerName();
        int serverPort = req.getServerPort();
        String contextPath = req.getContextPath();
        String servletPath = req.getServletPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append(servletPath);

        return url.toString();
    }
}