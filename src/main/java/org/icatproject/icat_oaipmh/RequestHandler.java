package org.icatproject.icat_oaipmh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Templates;

import org.icatproject.icat_oaipmh.exceptions.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private ResponseBuilder rb;

    private final boolean responseDebug;
    private final String responseStyle;

    public RequestHandler(String icatUrl, String[] icatAuth, String repositoryName, String[] adminEmails,
            String requestUrl, boolean responseDebug, String responseStyle) throws InternalException {
        ArrayList<String> emails = new ArrayList<String>(Arrays.asList(adminEmails));

        rb = new ResponseBuilder(icatUrl, icatAuth, repositoryName, emails, requestUrl);
        rb.loginIcat();

        this.responseDebug = responseDebug;
        this.responseStyle = responseStyle;
    }

    public void registerMetadataFormat(String identifier, MetadataFormat format) {
        rb.addMetadataFormat(identifier, format);
    }

    public void registerDataConfiguration(String identifier, DataConfiguration configuration) {
        rb.addDataConfiguration(identifier, configuration);
    }

    public void registerSet(String setSpec, ItemSet set) {
        rb.addSet(setSpec, set);
    }

    public String request(HttpServletRequest req) throws InternalException {
        Templates template = null;
        XmlResponse res = null;

        try {
            res = new XmlResponse();
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            throw new InternalException();
        }
        String[] verbs = req.getParameterValues("verb");

        if (verbs == null)
            handleIllegalVerb(req, res, "Missing verb argument");
        else if (verbs.length > 1)
            handleIllegalVerb(req, res, "Multiple verb arguments");
        else {
            String verb = verbs[0];
            if (verb.equals("Identify"))
                handleIdentify(req, res);
            else if (verb.equals("ListIdentifiers"))
                template = handleListIdentifiers(req, res);
            else if (verb.equals("ListRecords"))
                template = handleListRecords(req, res);
            else if (verb.equals("ListSets"))
                handleListSets(req, res);
            else if (verb.equals("ListMetadataFormats"))
                handleListMetadataFormats(req, res);
            else if (verb.equals("GetRecord"))
                template = handleGetRecord(req, res);
            else
                handleIllegalVerb(req, res, String.format("Illegal verb: %s", verb));
        }

        try {
            return res.transformXml(template);
        } catch (IllegalStateException e) {
            logger.error(e.getMessage());
            throw new InternalException();
        }
    }

    private void handleIdentify(HttpServletRequest req, XmlResponse res) throws InternalException {
        String[] allowedParameters = { "verb" };
        String[] requiredParameters = {};

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildIdentifyResponse(req, res);
        }
    }

    private Templates handleListIdentifiers(HttpServletRequest req, XmlResponse res) throws InternalException {
        String[] allowedParameters = { "verb", "from", "until", "metadataPrefix", "set", "resumptionToken" };
        String[] requiredParameters = { "metadataPrefix" };

        Templates template = null;
        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            template = getMetadataTemplate(req, res);
            if (template != null || responseDebug)
                rb.buildListIdentifiersResponse(req, res);
        }
        return template;
    }

    private Templates handleListRecords(HttpServletRequest req, XmlResponse res) throws InternalException {
        String[] allowedParameters = { "verb", "from", "until", "set", "resumptionToken", "metadataPrefix" };
        String[] requiredParameters = { "metadataPrefix" };

        Templates template = null;
        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            template = getMetadataTemplate(req, res);
            if (template != null || responseDebug)
                rb.buildListRecordsResponse(req, res);
        }
        return template;
    }

    private void handleListSets(HttpServletRequest req, XmlResponse res) throws InternalException {
        String[] allowedParameters = { "verb" };
        String[] requiredParameters = {};

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildListSetsResponse(req, res);
        }
    }

    private void handleListMetadataFormats(HttpServletRequest req, XmlResponse res) throws InternalException {
        String[] allowedParameters = { "verb", "identifier" };
        String[] requiredParameters = {};

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildListMetadataFormatsResponse(req, res);
        }
    }

    private Templates handleGetRecord(HttpServletRequest req, XmlResponse res) throws InternalException {
        String[] allowedParameters = { "verb", "identifier", "metadataPrefix" };
        String[] requiredParameters = { "identifier", "metadataPrefix" };

        Templates template = null;
        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            template = getMetadataTemplate(req, res);
            if (template != null || responseDebug)
                rb.buildGetRecordResponse(req, res);
        }
        return template;
    }

    private void handleIllegalVerb(HttpServletRequest req, XmlResponse res, String message) throws InternalException {
        res.makeResponseOutline(rb.getRequestUrl(), new HashMap<String, String>(), responseStyle);
        res.addError("badVerb", message);
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

        res.makeResponseOutline(rb.getRequestUrl(), checkedParameters, responseStyle);

        if (parameters.size() != checkedParameters.size()) {
            res.addError("badArgument", "The request includes illegal arguments, or includes a repeated argument");
            allParamsOk = false;
        }
        if (includesResumptionToken && parameters.size() != 2) {
            res.addError("badArgument", "The request includes illegal arguments in addition to the resumptionToken");
            allParamsOk = false;
        }

        if (!includesResumptionToken) {
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
        }

        return allParamsOk;
    }

    private Templates getMetadataTemplate(HttpServletRequest req, XmlResponse res) {
        String metadataPrefix = getMetadataPrefix(req);
        MetadataFormat metadataFormat = rb.getMetadataFormats().get(metadataPrefix);
        if (metadataFormat != null) {
            return metadataFormat.getTemplate();
        } else {
            res.addError("cannotDisseminateFormat", "'" + metadataPrefix + "' is not supported by the repository");
            return null;
        }
    }

    private String getMetadataPrefix(HttpServletRequest req) {
        if (req.getParameter("metadataPrefix") != null) {
            return req.getParameter("metadataPrefix");
        } else {
            String resumptionToken = req.getParameter("resumptionToken");
            return resumptionToken.split(",")[0];
        }
    }
}