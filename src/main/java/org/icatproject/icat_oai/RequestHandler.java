package org.icatproject.icat_oai;

import java.io.StringWriter;
import java.util.ArrayList;
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

    private ArrayList<MetadataFormat> metadataFormats;

    public RequestHandler() {
        metadataFormats = new ArrayList<MetadataFormat>();
    }

    public void registerMetadataFormat(MetadataFormat format) {
        metadataFormats.add(format);
    }

    public String request(HttpServletRequest req) {
        XmlResponse res = new XmlResponse();
        String verb = req.getParameter("verb");

        if (verb.equals("Identify"))
            identify(req, res);
        else if (verb.equals("ListIdentifiers"))
            listIdentifiers(req, res);
        else if (verb.equals("ListRecords"))
            listRecords(req, res);
        else if (verb.equals("ListSets"))
            listSets(req, res);
        else if (verb.equals("ListMetadataFormats"))
            listMetadataFormats(req, res);
        else if (verb.equals("GetRecord"))
            getRecord(req, res);
        else
            illegalVerb(req, res);

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

    private void identify(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb" };
        String[] requiredParameters = {};
        checkParameters(allowedParameters, requiredParameters, req, res);
    }

    private void listIdentifiers(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "from", "until", "metadataPrefix", "set", "resumptionToken" };
        String[] requiredParameters = { "metadataPrefix" };
        checkParameters(allowedParameters, requiredParameters, req, res);
    }

    private void listRecords(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "from", "until", "set", "resumptionToken", "metadataPrefix" };
        String[] requiredParameters = { "metadataPrefix" };
        checkParameters(allowedParameters, requiredParameters, req, res);
    }

    private void listSets(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "resumptionToken" };
        String[] requiredParameters = {};
        checkParameters(allowedParameters, requiredParameters, req, res);
    }

    private void listMetadataFormats(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "identifier" };
        String[] requiredParameters = {};
        checkParameters(allowedParameters, requiredParameters, req, res);
    }

    private void getRecord(HttpServletRequest req, XmlResponse res) {
        String[] allowedParameters = { "verb", "identifier", "metadataPrefix" };
        String[] requiredParameters = { "identifier", "metadataPrefix" };
        checkParameters(allowedParameters, requiredParameters, req, res);
    }

    private void illegalVerb(HttpServletRequest req, XmlResponse res) {
        res.makeResponseOutline(getRequestUrl(req), new HashMap<String, String>());
        res.addError("badVerb", "Illegal verb: " + req.getParameter("verb"));
    }

    private void checkParameters(String[] allowedParameters, String[] requiredParameters, HttpServletRequest req,
            XmlResponse res) {
        Map<String, String> checkedParameters = new HashMap<String, String>();
        Map<String, String[]> parameters = req.getParameterMap();

        boolean ok = false;
        boolean includesResumptionToken = false;

        for (String param : parameters.keySet()) {
            ok = false;
            if (param.equals("resumptionToken"))
                includesResumptionToken = true;
            if (parameters.get(param).length == 1) {
                for (String allowedParameter : allowedParameters) {
                    if (allowedParameter.contains(param)) {
                        ok = true;
                        // break;
                    }
                }
            }
            if (ok)
                checkedParameters.put(param, parameters.get(param)[0]);
        }

        res.makeResponseOutline(getRequestUrl(req), checkedParameters);

        if (parameters.size() != checkedParameters.size())
            res.addError("badArgument", "The request includes illegal arguments, or includes a repeated argument.");
        if (includesResumptionToken && parameters.size() != 2)
            res.addError("badArgument", "The request includes illegal arguments in addition to the resumptionToken.");

        for (String requiredParameter : requiredParameters) {
            ok = false;
            for (String param : parameters.keySet()) {
                if (requiredParameter.contains(param)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                res.addError("badArgument", "The request is missing the required argument: " + requiredParameter);
            }
        }
    }

    private String getRequestUrl(HttpServletRequest req) {
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