package org.icatproject.icat_oai;

import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.servlet.http.HttpServletRequest;

import org.icatproject.icat.client.ICAT;
import org.icatproject.icat.client.IcatException;
import org.icatproject.icat.client.Session;
import org.icatproject.icat_oai.exceptions.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class ResponseBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ResponseBuilder.class);

    private Session icatSession;
    private final ArrayList<String> adminEmails;
    private final DataConfiguration dataConfiguration;
    private ArrayList<MetadataFormat> metadataFormats;

    public ResponseBuilder(ArrayList<String> adminEmails, DataConfiguration dataConfiguration) {
        metadataFormats = new ArrayList<MetadataFormat>();
        this.adminEmails = adminEmails;
        this.dataConfiguration = dataConfiguration;
    }

    public void performIcatLogin(String icatUrl, String[] icatAuth) throws InternalException {
        ICAT restIcat = null;
        try {
            restIcat = new ICAT(icatUrl);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage());
            throw new InternalException();
        }

        HashMap<String, String> credentials = new HashMap<String, String>();
        credentials.put(icatAuth[1], icatAuth[2]);
        credentials.put(icatAuth[3], icatAuth[4]);

        try {
            icatSession = restIcat.login(icatAuth[0], credentials);
        } catch (IcatException e) {
            logger.error(e.getMessage());
            throw new InternalException();
        }
    }

    public void addMetadataFormat(MetadataFormat format) {
        metadataFormats.add(format);
    }

    public ArrayList<MetadataFormat> getMetadataFormats() {
        return metadataFormats;
    }

    public void buildIdentifyResponse(HttpServletRequest req, XmlResponse res) throws InternalException {
        HashMap<String, String> singleProperties = new HashMap<String, String>();
        HashMap<String, ArrayList<String>> repeatedProperties = new HashMap<String, ArrayList<String>>();

        String repositoryName = "Facility Repository";
        String earliestDatestamp = "1000-01-01T00:00:00Z";
        try {
            String result;
            JsonReader jsonReader;
            JsonArray jsonArray;

            result = icatSession.search("SELECT f.fullName FROM Facility f");
            jsonReader = Json.createReader(new java.io.StringReader(result));
            jsonArray = jsonReader.readArray();
            jsonReader.close();
            repositoryName = jsonArray.getJsonString(0).getString();

            String query = String.format("SELECT d.modTime FROM %s d ORDER BY d.modTime",
                    dataConfiguration.getMainObject());
            result = icatSession.search(query);
            jsonReader = Json.createReader(new java.io.StringReader(result));
            jsonArray = jsonReader.readArray();
            jsonReader.close();
            String earliestDate = jsonArray.getJsonString(0).getString();
            earliestDatestamp = getFormattedDateTime(earliestDate);
        } catch (IndexOutOfBoundsException e) {
            logger.error(e.getMessage());
        } catch (IcatException e) {
            logger.error(e.getMessage());
            throw new InternalException();
        }

        singleProperties.put("repositoryName", repositoryName);
        singleProperties.put("baseURL", getRequestUrl(req));
        singleProperties.put("protocolVersion", "2.0");
        singleProperties.put("earliestDatestamp", earliestDatestamp);
        singleProperties.put("deletedRecord", "transient");
        singleProperties.put("granularity", "YYYY-MM-DDThh:mm:ssZ");

        repeatedProperties.put("adminEmail", adminEmails);
        XmlInformation info = new XmlInformation(singleProperties, repeatedProperties, null);

        res.addXmlInformation(info, "Identify", null);
    }

    public void buildListIdentifiersResponse(HttpServletRequest req, XmlResponse res) throws InternalException {
        IcatQueryParameters parameters = getIcatQueryParameters(req, res);

        if (parameters != null) {
            IcatQueryResults results = getIcatHeaders(parameters);

            Element listIdentifiers = null;
            if (results.getResults().isEmpty()) {
                res.addError("noRecordsMatch",
                        "The combination of the values of the from, until, and set arguments results in an empty list");
            } else {
                listIdentifiers = res.addRecordInformation(results.getResults(), "ListIdentifiers", false);
            }

            // fixme: move to shared function
            if (results.getIncomplete()) {
                String metadataPrefix = getMetadataPrefix(req);
                String resumptionToken = parameters.makeResumptionToken(metadataPrefix);

                HashMap<String, String> singleProperties = new HashMap<String, String>();
                singleProperties.put("resumptionToken", resumptionToken);
                XmlInformation info = new XmlInformation(singleProperties, null, null);
                res.addXmlInformation(info, null, listIdentifiers);
            }
        }
    }

    public void buildListRecordsResponse(HttpServletRequest req, XmlResponse res) throws InternalException {
        IcatQueryParameters parameters = getIcatQueryParameters(req, res);

        if (parameters != null) {
            IcatQueryResults results = getIcatRecords(parameters);

            Element listRecords = null;
            if (results.getResults().isEmpty()) {
                res.addError("noRecordsMatch",
                        "The combination of the values of the from, until, and set arguments results in an empty list");
            } else {
                listRecords = res.addRecordInformation(results.getResults(), "ListRecords", true);
            }

            // fixme: move to shared function
            if (results.getIncomplete()) {
                String metadataPrefix = getMetadataPrefix(req);
                String resumptionToken = parameters.makeResumptionToken(metadataPrefix);
                HashMap<String, String> singleProperties = new HashMap<String, String>();
                singleProperties.put("resumptionToken", resumptionToken);
                XmlInformation info = new XmlInformation(singleProperties, null, null);
                res.addXmlInformation(info, null, listRecords);
            }
        }
    }

    public void buildListSetsResponse(HttpServletRequest req, XmlResponse res) {
        res.addError("noSetHierarchy", "This repository does not support sets");
    }

    public void buildListMetadataFormatsResponse(HttpServletRequest req, XmlResponse res) {
        HashMap<String, ArrayList<XmlInformation>> informationLists = new HashMap<String, ArrayList<XmlInformation>>();

        ArrayList<XmlInformation> metadataFormatInfo = new ArrayList<XmlInformation>();

        for (MetadataFormat format : metadataFormats) {
            HashMap<String, String> singleProperties = new HashMap<String, String>();

            singleProperties.put("metadataPrefix", format.getMetadataPrefix());
            singleProperties.put("metadataNamespace", format.getMetadataNamespace());
            singleProperties.put("schema", format.getMetadataSchema());

            metadataFormatInfo.add(new XmlInformation(singleProperties, null, null));
        }

        informationLists.put("metadataFormat", metadataFormatInfo);

        XmlInformation info = new XmlInformation(null, null, informationLists);
        res.addXmlInformation(info, "ListMetadataFormats", null);
    }

    public void buildGetRecordResponse(HttpServletRequest req, XmlResponse res) throws InternalException {
        IcatQueryParameters parameters = getIcatQueryParameters(req, res);

        if (parameters != null) {
            IcatQueryResults result = getIcatRecords(parameters);

            if (result.getResults().isEmpty()) {
                res.addError("idDoesNotExist",
                        "Identifier '" + req.getParameter("identifier") + "' is unknown or illegal in this repository");
            } else {
                res.addRecordInformation(result.getResults(), "GetRecord", true);
            }
        }
    }

    private IcatQueryParameters getIcatQueryParameters(HttpServletRequest req, XmlResponse res)
            throws InternalException {
        IcatQueryParameters parameters = null;

        String identifierPrefix = req.getServerName();
        String resumptionToken = req.getParameter("resumptionToken");
        if (resumptionToken != null) {
            try {
                parameters = new IcatQueryParameters(resumptionToken, identifierPrefix);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                res.addError("badResumptionToken", "The value of the resumptionToken argument is invalid");
            }
        } else {
            String identifier = req.getParameter("identifier");
            String from = req.getParameter("from");
            String until = req.getParameter("until");
            try {
                parameters = new IcatQueryParameters(0, from, until, identifier, identifierPrefix);
            } catch (ArrayIndexOutOfBoundsException | DateTimeException e) {
                res.addError("badArgument", "The request includes illegally formatted arguments");
            }
        }

        return parameters;
    }

    public IcatQueryResults getIcatHeaders(IcatQueryParameters parameters) throws InternalException {
        ArrayList<RecordInformation> headers = new ArrayList<RecordInformation>();

        IcatQuery query = performIcatQuery(parameters);
        for (JsonValue data : query.getResults()) {
            boolean deleted = extractDeletedStatus(data, dataConfiguration.getDeletedIfAllNull());
            XmlInformation header = extractHeaderInformation(data, parameters.getIdentifierPrefix());
            headers.add(new RecordInformation(deleted, header, null));
        }

        return new IcatQueryResults(headers, query.getIncomplete());
    }

    public IcatQueryResults getIcatRecords(IcatQueryParameters parameters) throws InternalException {
        ArrayList<RecordInformation> records = new ArrayList<RecordInformation>();

        IcatQuery query = performIcatQuery(parameters);
        for (JsonValue data : query.getResults()) {
            boolean deleted = extractDeletedStatus(data, dataConfiguration.getDeletedIfAllNull());
            XmlInformation header = extractHeaderInformation(data, parameters.getIdentifierPrefix());
            XmlInformation metadata = null;
            if (!deleted)
                metadata = extractMetadataInformation(data, dataConfiguration.getRequestedProperties()).get(0);

            records.add(new RecordInformation(deleted, header, metadata));
        }

        return new IcatQueryResults(records, query.getIncomplete());
    }

    private IcatQuery performIcatQuery(IcatQueryParameters parameters) throws InternalException {
        String includes = String.join(", d.", dataConfiguration.getIncludedObjects());
        String where = parameters.makeWhereCondition();
        String query = String.format("SELECT d FROM %s d %s ORDER BY d.modTime INCLUDE d.%s",
                dataConfiguration.getMainObject(), where, includes);

        JsonArray resultsArray = null;
        try {
            String result = icatSession.search(query);
            JsonReader jsonReader = Json.createReader(new java.io.StringReader(result));
            resultsArray = jsonReader.readArray();
            jsonReader.close();
        } catch (IcatException e) {
            logger.error(e.getMessage());
            throw new InternalException();
        }

        boolean incomplete = false;
        List<JsonValue> results = null;
        results = resultsArray.subList(parameters.getOffset(), resultsArray.size());
        if (results.size() > parameters.getOffsetSize()) {
            incomplete = true;
            int limit = parameters.getOffsetSize();
            if (limit > results.size())
                limit = results.size();
            results = results.subList(0, limit);
        }

        return new IcatQuery(results, incomplete);
    }

    private boolean extractDeletedStatus(JsonValue data, List<String> deletedIfAllNull) {
        if (deletedIfAllNull.isEmpty())
            return false;

        List<String> remaining = deletedIfAllNull.subList(1, deletedIfAllNull.size());
        String current = deletedIfAllNull.get(0);

        if (remaining.isEmpty()) {
            JsonValue value = ((JsonObject) data).get(current);
            return (value == null);
        }

        ValueType valueType = ((JsonObject) data).get(current).getValueType();
        if (valueType == ValueType.ARRAY) {
            JsonArray jsonArray = ((JsonObject) data).getJsonArray(current);
            for (JsonValue subObject : jsonArray) {
                if (!extractDeletedStatus(subObject, remaining))
                    return false;
            }
            return true;
        } else {
            JsonObject jsonObject = ((JsonObject) data).getJsonObject(current);
            return (extractDeletedStatus(jsonObject, remaining));
        }
    }

    private XmlInformation extractHeaderInformation(JsonValue data, String identifierPrefix) {
        HashMap<String, String> singleProperties = new HashMap<String, String>();

        JsonObject icatObject = ((JsonObject) data)
                .getJsonObject(dataConfiguration.getRequestedProperties().getIcatObject());

        singleProperties.put("identifier", getFormattedIdentifier(identifierPrefix, icatObject.get("id").toString()));
        singleProperties.put("datestamp", getFormattedDateTime(icatObject.getString("modTime", null)));

        XmlInformation headers = new XmlInformation(singleProperties, null, null);
        return headers;
    }

    private ArrayList<XmlInformation> extractMetadataInformation(JsonValue data,
            RequestedProperties requestedProperties) throws InternalException {
        ArrayList<XmlInformation> result = new ArrayList<XmlInformation>();

        HashMap<String, String> singleProperties = new HashMap<String, String>();
        HashMap<String, ArrayList<XmlInformation>> informationLists = new HashMap<String, ArrayList<XmlInformation>>();

        JsonValue icatObject = ((JsonObject) data).get(requestedProperties.getIcatObject());
        if (icatObject == null) {
            logger.error("The requested ICAT object '" + requestedProperties.getIcatObject() + "' was not found");
            throw new InternalException();
        }

        if (icatObject.getValueType() == ValueType.ARRAY) {
            JsonArray jsonArray = (JsonArray) icatObject;

            ArrayList<XmlInformation> elementsInfos = new ArrayList<XmlInformation>();
            for (JsonValue element : jsonArray) {
                HashMap<String, String> elementSingleProperties = new HashMap<String, String>();
                HashMap<String, ArrayList<XmlInformation>> elementInformationLists = new HashMap<String, ArrayList<XmlInformation>>();

                for (String prop : requestedProperties.getStringProperties()) {
                    String value = ((JsonObject) element).getString(prop, null);
                    if (value != null) {
                        elementSingleProperties.put(prop, value);
                    }
                }

                for (String prop : requestedProperties.getNumericProperties()) {
                    JsonValue value = ((JsonObject) element).get(prop);
                    if (value != null)
                        elementSingleProperties.put(prop, value.toString());
                }

                for (String prop : requestedProperties.getDateProperties()) {
                    String value = getFormattedDateTime(((JsonObject) element).getString(prop, null));
                    if (value != null)
                        elementSingleProperties.put(prop, value);
                }

                for (RequestedProperties requestedSubProperties : requestedProperties.getSubPropertyLists()) {
                    ArrayList<XmlInformation> subInfo = extractMetadataInformation(element, requestedSubProperties);
                    elementInformationLists.put(requestedSubProperties.getIcatObject(), subInfo);
                }

                if (elementSingleProperties.size() != 0 || elementInformationLists.size() != 0)
                    elementsInfos.add(new XmlInformation(elementSingleProperties, null, elementInformationLists));
            }
            informationLists.put("instance", elementsInfos);
        } else {
            JsonObject jsonObject = (JsonObject) icatObject;

            for (String prop : requestedProperties.getStringProperties()) {
                String value = jsonObject.getString(prop, null);
                if (value != null)
                    singleProperties.put(prop, value);
            }

            for (String prop : requestedProperties.getNumericProperties()) {
                JsonValue value = jsonObject.get(prop);
                if (value != null)
                    singleProperties.put(prop, value.toString());
            }

            for (String prop : requestedProperties.getDateProperties()) {
                String value = getFormattedDateTime(jsonObject.getString(prop, null));
                if (value != null)
                    singleProperties.put(prop, value);
            }

            for (RequestedProperties requestedSubProperties : requestedProperties.getSubPropertyLists()) {
                ArrayList<XmlInformation> info = extractMetadataInformation(jsonObject, requestedSubProperties);
                informationLists.put(requestedSubProperties.getIcatObject(), info);
            }
        }

        result.add(new XmlInformation(singleProperties, null, informationLists));
        return result;
    }

    private String getFormattedIdentifier(String url, String id) {
        return "oai:" + url.toString() + ":" + id.toString();
    }

    private String getFormattedDateTime(String dateTime) {
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

    public String getMetadataPrefix(HttpServletRequest req) {
        if (req.getParameter("metadataPrefix") != null) {
            return req.getParameter("metadataPrefix");
        } else {
            String resumptionToken = req.getParameter("resumptionToken");
            return resumptionToken.split(",")[0];
        }
    }
}