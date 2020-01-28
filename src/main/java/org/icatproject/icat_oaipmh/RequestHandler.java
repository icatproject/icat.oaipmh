package org.icatproject.icat_oaipmh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Templates;

import org.icatproject.icat_oaipmh.exceptions.InternalException;

public class RequestHandler {

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

    public String request(HttpServletRequest req) throws InternalException {
        XmlResponse res = new XmlResponse();
        String[] verbs = req.getParameterValues("verb");
        Templates template = null;

        if (verbs != null && verbs.length == 1) {
            String verb = verbs[0];
            if (verb.equals("Identify"))
                return handleIdentify(req, res, template);
            if (verb.equals("ListIdentifiers"))
                return handleListIdentifiers(req, res, template);
            if (verb.equals("ListRecords"))
                return handleListRecords(req, res, template);
            if (verb.equals("ListSets"))
                return handleListSets(req, res, template);
            if (verb.equals("ListMetadataFormats"))
                return handleListMetadataFormats(req, res, template);
            if (verb.equals("GetRecord"))
                return handleGetRecord(req, res, template);
        }
        return handleIllegalVerb(req, res, template);
    }

    private String handleIdentify(HttpServletRequest req, XmlResponse res, Templates template)
            throws InternalException {
        String[] allowedParameters = { "verb" };
        String[] requiredParameters = {};

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildIdentifyResponse(req, res);
        }

        return res.transformXml(template);
    }

    private String handleListIdentifiers(HttpServletRequest req, XmlResponse res, Templates template)
            throws InternalException {
        String[] allowedParameters = { "verb", "from", "until", "metadataPrefix", "set", "resumptionToken" };
        String[] requiredParameters = { "metadataPrefix" };

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            template = getMetadataTemplate(req, res);
            if (template != null || responseDebug)
                rb.buildListIdentifiersResponse(req, res);
        }

        return res.transformXml(template);
    }

    private String handleListRecords(HttpServletRequest req, XmlResponse res, Templates template)
            throws InternalException {
        String[] allowedParameters = { "verb", "from", "until", "set", "resumptionToken", "metadataPrefix" };
        String[] requiredParameters = { "metadataPrefix" };

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            template = getMetadataTemplate(req, res);
            if (template != null || responseDebug)
                rb.buildListRecordsResponse(req, res);
        }

        return res.transformXml(template);
    }

    private String handleListSets(HttpServletRequest req, XmlResponse res, Templates template)
            throws InternalException {
        String[] allowedParameters = { "verb", "resumptionToken" };
        String[] requiredParameters = {};

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildListSetsResponse(req, res);
        }

        return res.transformXml(template);
    }

    private String handleListMetadataFormats(HttpServletRequest req, XmlResponse res, Templates template)
            throws InternalException {
        String[] allowedParameters = { "verb", "identifier" };
        String[] requiredParameters = {};

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            rb.buildListMetadataFormatsResponse(req, res);
        }

        return res.transformXml(template);
    }

    private String handleGetRecord(HttpServletRequest req, XmlResponse res, Templates template)
            throws InternalException {
        String[] allowedParameters = { "verb", "identifier", "metadataPrefix" };
        String[] requiredParameters = { "identifier", "metadataPrefix" };

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            template = getMetadataTemplate(req, res);
            if (template != null || responseDebug)
                rb.buildGetRecordResponse(req, res);
        }

        return res.transformXml(template);
    }

    private String handleIllegalVerb(HttpServletRequest req, XmlResponse res, Templates template)
            throws InternalException {
        res.makeResponseOutline(rb.getRequestUrl(), new HashMap<String, String>(), responseStyle);
        res.addError("badVerb", "Illegal verb: " + req.getParameter("verb"));
        return res.transformXml(template);
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