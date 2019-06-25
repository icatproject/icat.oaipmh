package org.icatproject.icat_oai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Templates;

import org.icatproject.icat_oai.exceptions.InternalException;

public class RequestHandler {

    private ResponseBuilder rb;
    private boolean debug;

    public RequestHandler(String icatUrl, String[] icatAuth, String repositoryName, String[] adminEmails,
            DataConfiguration dataConfiguration, boolean debug) throws InternalException {
        ArrayList<String> emails = new ArrayList<String>(Arrays.asList(adminEmails));

        rb = new ResponseBuilder(repositoryName, emails, dataConfiguration);
        rb.performIcatLogin(icatUrl, icatAuth);

        this.debug = debug;
    }

    public void registerMetadataFormat(MetadataFormat format) {
        rb.addMetadataFormat(format);
    }

    public String request(HttpServletRequest req) throws InternalException {
        XmlResponse res = new XmlResponse();
        String verb = req.getParameter("verb");
        Templates template = null;

        if (verb.equals("Identify"))
            return handleIdentify(req, res, template);
        else if (verb.equals("ListIdentifiers"))
            return handleListIdentifiers(req, res, template);
        else if (verb.equals("ListRecords"))
            return handleListRecords(req, res, template);
        else if (verb.equals("ListSets"))
            return handleListSets(req, res, template);
        else if (verb.equals("ListMetadataFormats"))
            return handleListMetadataFormats(req, res, template);
        else if (verb.equals("GetRecord"))
            return handleGetRecord(req, res, template);
        else
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
            if (template != null)
                rb.buildListIdentifiersResponse(req, res);
        }

        return res.transformXml(debug ? null : template);
    }

    private String handleListRecords(HttpServletRequest req, XmlResponse res, Templates template)
            throws InternalException {
        String[] allowedParameters = { "verb", "from", "until", "set", "resumptionToken", "metadataPrefix" };
        String[] requiredParameters = { "metadataPrefix" };

        if (checkParameters(allowedParameters, requiredParameters, req, res)) {
            template = getMetadataTemplate(req, res);
            if (template != null)
                rb.buildListRecordsResponse(req, res);
        }

        return res.transformXml(debug ? null : template);
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
            if (template != null)
                rb.buildGetRecordResponse(req, res);
        }

        return res.transformXml(debug ? null : template);
    }

    private String handleIllegalVerb(HttpServletRequest req, XmlResponse res, Templates template)
            throws InternalException {
        res.makeResponseOutline(rb.getRequestUrl(req), new HashMap<String, String>());
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

        res.makeResponseOutline(rb.getRequestUrl(req), checkedParameters);

        if (parameters.size() != checkedParameters.size())
            res.addError("badArgument", "The request includes illegal arguments, or includes a repeated argument");
        if (includesResumptionToken && parameters.size() != 2)
            res.addError("badArgument", "The request includes illegal arguments in addition to the resumptionToken");

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
        String metadataPrefix = rb.getMetadataPrefix(req);
        for (MetadataFormat format : rb.getMetadataFormats()) {
            if (metadataPrefix.equals(format.getMetadataPrefix())) {
                return format.getTemplate();
            }
        }
        res.addError("cannotDisseminateFormat", "'" + metadataPrefix + "' is not supported by the repository");
        return null;
    }
}