package org.icatproject.icat_oaipmh;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlInformation {

    private Map<String, ? extends List<String>> properties;
    private Map<String, ? extends List<XmlInformation>> informationLists;

    public XmlInformation(Map<String, ? extends List<String>> properties,
            Map<String, ? extends List<XmlInformation>> informationLists) {
        if (properties != null)
            this.properties = properties;
        else
            this.properties = new HashMap<String, List<String>>();

        if (informationLists != null)
            this.informationLists = informationLists;
        else
            this.informationLists = new HashMap<String, List<XmlInformation>>();
    }

    public Map<String, ? extends List<String>> getProperties() {
        return properties;
    }

    public Map<String, ? extends List<XmlInformation>> getInformationLists() {
        return informationLists;
    }
}