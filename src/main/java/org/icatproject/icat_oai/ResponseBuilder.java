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
import javax.json.JsonValue;
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

        ArrayList<HeaderInformation> headers = getIcatHeaders(req, res);
        for (HeaderInformation header : headers) {
            appendXmlHeader(listIdentifiers, header);
        }
        res.addContent(listIdentifiers);
    }

    public void buildListRecordsResponse(HttpServletRequest req, XmlResponse res) {
        Document doc = res.getDocument();

        Element listRecords = doc.createElement("ListRecords");

        ArrayList<RecordInformation> records = getIcatRecords(req, res);
        for (RecordInformation info : records) {
            Element record = doc.createElement("record");
            appendXmlHeader(record, info.getHeader());
            appendXmlMetadata(record, info.getMetadata());
            listRecords.appendChild(record);
        }
        res.addContent(listRecords);
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

    public void buildGetRecordResponse(HttpServletRequest req, XmlResponse res) {
        Document doc = res.getDocument();

        Element getRecord = doc.createElement("GetRecord");

        RecordInformation info = getIcatRecords(req, res).get(0);

        Element record = doc.createElement("record");
        appendXmlHeader(record, info.getHeader());
        appendXmlMetadata(record, info.getMetadata());
        getRecord.appendChild(record);

        res.addContent(getRecord);
    }

    public ArrayList<HeaderInformation> getIcatHeaders(HttpServletRequest req, XmlResponse res) {
        ArrayList<HeaderInformation> headers = new ArrayList<HeaderInformation>();

        try {
            String result = icatSession
                    .search("SELECT inv.id, inv.modTime FROM Investigation inv ORDER BY inv.modTime");
            JsonReader jsonReader = Json.createReader(new java.io.StringReader(result));
            JsonArray jsonArray = jsonReader.readArray();
            jsonReader.close();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonArray item = jsonArray.getJsonArray(i);

                String identifier = getFormattedIdentifier(req.getServerName(), item.getJsonNumber(0).toString());
                String datestamp = getFormattedDateTime(item.getJsonString(1).getString());
                boolean deleted = false;

                headers.add(new HeaderInformation(identifier, datestamp, deleted));
            }
        } catch (IcatException e) {
            logger.error(e.getMessage());
        }

        return headers;
    }

    public ArrayList<RecordInformation> getIcatRecords(HttpServletRequest req, XmlResponse res) {
        ArrayList<RecordInformation> records = new ArrayList<RecordInformation>();

        try {
            String result = icatSession.search(
                    "SELECT inv.id, inv.modTime, inv.doi, inv.title, inv.releaseDate, inv.startDate, inv.endDate, inv.name, inv.visitId, inv.summary FROM Investigation inv ORDER BY inv.modTime");
            JsonReader jsonReader = Json.createReader(new java.io.StringReader(result));
            JsonArray jsonArray = jsonReader.readArray();
            jsonReader.close();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonArray item = jsonArray.getJsonArray(i);

                String identifier = getFormattedIdentifier(req.getServerName(), item.getJsonNumber(0).toString());
                String datestamp = getFormattedDateTime(item.getJsonString(1).getString());
                boolean deleted = false;
                HeaderInformation header = new HeaderInformation(identifier, datestamp, deleted);

                String doi = getJsonAsString(item.get(2));
                ArrayList<MetadataUser> investigationUsers = new ArrayList<MetadataUser>();
                String title = getJsonAsString(item.get(3));
                String releaseDate = getFormattedDateTime(getJsonAsString(item.get(4)));
                String startDate = getFormattedDateTime(getJsonAsString(item.get(5)));
                String endDate = getFormattedDateTime(getJsonAsString(item.get(6)));
                String name = getJsonAsString(item.get(7));
                String visitId = getJsonAsString(item.get(8));
                String summary = getJsonAsString(item.get(9));
                MetadataInformation metadata = new MetadataInformation(doi, investigationUsers, title, releaseDate,
                        startDate, endDate, name, visitId, summary);

                records.add(new RecordInformation(header, metadata));
            }
        } catch (IcatException e) {
            logger.error(e.getMessage());
        }

        return records;
    }

    public void appendXmlHeader(Element el, HeaderInformation info) {
        Document doc = el.getOwnerDocument();
        Element header = doc.createElement("header");

        if (info.getDeleted()) {
            header.setAttribute("status", "deleted");
        }

        Element id = doc.createElement("identifier");
        id.setTextContent(info.getIdentifier());
        header.appendChild(id);

        Element date = doc.createElement("datestamp");
        date.setTextContent(info.getDatestamp());
        header.appendChild(date);

        el.appendChild(header);
    }

    public void appendXmlMetadata(Element el, MetadataInformation info) {
        Document doc = el.getOwnerDocument();
        Element metadata = doc.createElement("metadata");

        if (info.getDoi() != null) {
            Element doi = doc.createElement("doi");
            doi.setTextContent(info.getDoi());
            metadata.appendChild(doi);
        }

        if (info.getInvestigationUsers().size() != 0) {
            Element users = doc.createElement("users");
            for (MetadataUser investigationUser : info.getInvestigationUsers()) {
                Element user = doc.createElement("user");

                if (investigationUser.getFullName() != null) {
                    Element fullName = doc.createElement("fullName");
                    fullName.setTextContent(investigationUser.getFullName());
                    user.appendChild(fullName);
                }

                if (investigationUser.getGivenName() != null) {
                    Element givenName = doc.createElement("givenName");
                    givenName.setTextContent(investigationUser.getGivenName());
                    user.appendChild(givenName);
                }

                if (investigationUser.getFamilyName() != null) {
                    Element familyName = doc.createElement("familyName");
                    familyName.setTextContent(investigationUser.getFamilyName());
                    user.appendChild(familyName);
                }

                if (investigationUser.getOrcidId() != null) {
                    Element orcidId = doc.createElement("orcidId");
                    orcidId.setTextContent(investigationUser.getOrcidId());
                    user.appendChild(orcidId);
                }

                users.appendChild(user);
            }
            metadata.appendChild(users);
        }

        if (info.getTitle() != null) {
            Element title = doc.createElement("title");
            title.setTextContent(info.getTitle());
            metadata.appendChild(title);
        }

        if (info.getReleaseDate() != null) {
            Element releaseDate = doc.createElement("releaseDate");
            releaseDate.setTextContent(info.getReleaseDate());
            metadata.appendChild(releaseDate);
        }

        if (info.getStartDate() != null) {
            Element startDate = doc.createElement("startDate");
            startDate.setTextContent(info.getStartDate());
            metadata.appendChild(startDate);
        }

        if (info.getEndDate() != null) {
            Element endDate = doc.createElement("endDate");
            endDate.setTextContent(info.getEndDate());
            metadata.appendChild(endDate);
        }

        if (info.getName() != null) {
            Element name = doc.createElement("name");
            name.setTextContent(info.getName());
            metadata.appendChild(name);
        }

        if (info.getVisitId() != null) {
            Element visitId = doc.createElement("visitId");
            visitId.setTextContent(info.getVisitId());
            metadata.appendChild(visitId);
        }

        if (info.getSummary() != null) {
            Element summary = doc.createElement("summary");
            summary.setTextContent(info.getSummary());
            metadata.appendChild(summary);
        }

        el.appendChild(metadata);
    }

    public String getJsonAsString(JsonValue value) {
        switch (value.getValueType()) {
        case STRING:
            return ((JsonString) value).getString();
        case NULL:
            return null;
        default:
            return value.toString();
        }
    }

    public String getFormattedIdentifier(String url, String id) {
        return "oai:" + url.toString() + ":" + id.toString();
    }

    public String getFormattedDateTime(String dateTime) {
        if (dateTime != null)
            return OffsetDateTime.parse(dateTime).format(DateTimeFormatter.ISO_INSTANT);
        return null;
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