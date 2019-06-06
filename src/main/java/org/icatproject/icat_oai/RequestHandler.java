package org.icatproject.icat_oai;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private ResponseBuilder rb;

    public RequestHandler(String icatUrl, String[] icatAuth, String repositoryName, String[] adminEmails) {
        rb = new ResponseBuilder(repositoryName, adminEmails);
        rb.performIcatLogin(icatUrl, icatAuth);
    }

    public void registerMetadataFormat(MetadataFormat format) {
        rb.addMetadataFormat(format);
    }

    public String request(HttpServletRequest req) {
        XmlResponse res = new XmlResponse();
        String verb = req.getParameter("verb");

        if (verb.equals("Identify"))
            handleIdentify(req, res);
        else if (verb.equals("ListIdentifiers"))
            handleListIdentifiers(req, res);
        else if (verb.equals("ListRecords"))
            handleListRecords(req, res);
        else if (verb.equals("ListSets"))
            handleListSets(req, res);
        else if (verb.equals("ListMetadataFormats"))
            handleListMetadataFormats(req, res);
        else if (verb.equals("GetRecord"))
            handleGetRecord(req, res);
        else
            handleIllegalVerb(req, res);

        String output = "";
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(res.getDocument());
            StringWriter writer = new StringWriter();
            transformer.transform(source, new StreamResult(writer));
            output = writer.getBuffer().toString();
        } catch (TransformerException e) {
            logger.error(e.getMessage());
        }
        return output;
    }

    private void handleIdentify(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb" };
        String[] requiredParameters = {};

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildIdentifyResponse(req, res);
        }
    }

    private void handleListIdentifiers(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "from", "until", "metadataPrefix", "set", "resumptionToken" };
        String[] requiredParameters = { "metadataPrefix" };

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildListIdentifiersResponse(req, res);
        }
    }

    private void handleListRecords(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "from", "until", "set", "resumptionToken", "metadataPrefix" };
        String[] requiredParameters = { "metadataPrefix" };

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildListRecordsResponse(req, res);
        }
    }

    private void handleListSets(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "resumptionToken" };
        String[] requiredParameters = {};

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildListSetsResponse(req, res);
        }
    }

    private void handleListMetadataFormats(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "identifier" };
        String[] requiredParameters = {};

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildListMetadataFormatsResponse(req, res);
        }
    }

    private void handleGetRecord(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "identifier", "metadataPrefix" };
        String[] requiredParameters = { "identifier", "metadataPrefix" };

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildGetRecordResponse(req, res);
        }
    }

    private void handleIllegalVerb(HttpServletRequest req, XmlResponse res) {
        res.makeResponseOutline(rb.getRequestUrl(req), new HashMap<String, String>());
        res.addError("badVerb", "Illegal verb: " + req.getParameter("verb"));
    }

    private boolean checkParameters(String[] allowedParameters, String[] requiredParameters, HttpServletRequest req,
            XmlResponse res) {
        Map<String, String> checkedParameters = new HashMap<String, String>();
        Map<String, String[]> parameters = req.getParameterMap();

        boolean allParamsOk = true;
        boolean paramOk = false;
        boolean includesResumptionToken = false;

        for (String param : parameters.keySet()) {
            paramOk = false;
            if (param.equals("resumptionToken"))
                includesResumptionToken = true;
            if (parameters.get(param).length == 1) {
                for (String allowedParameter : allowedParameters) {
                    if (allowedParameter.contains(param)) {
                        paramOk = true;
                        break;
                    }
                }
            }
            if (paramOk)
                checkedParameters.put(param, parameters.get(param)[0]);
            else
                allParamsOk = false;
        }

        res.makeResponseOutline(rb.getRequestUrl(req), checkedParameters);

        if (parameters.size() != checkedParameters.size())
            res.addError("badArgument", "The request includes illegal arguments, or includes a repeated argument.");
        if (includesResumptionToken && parameters.size() != 2)
            res.addError("badArgument", "The request includes illegal arguments in addition to the resumptionToken.");

        for (String requiredParameter : requiredParameters) {
            paramOk = false;
            for (String param : parameters.keySet()) {
                if (requiredParameter.contains(param)) {
                    paramOk = true;
                    break;
                }
            }
            if (!paramOk) {
                res.addError("badArgument", "The request is missing the required argument: " + requiredParameter);
                allParamsOk = false;
            }
        }

        return allParamsOk;
    }
}