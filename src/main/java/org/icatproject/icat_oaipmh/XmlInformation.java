package org.icatproject.icat_oaipmh;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlInformation {

    private Map<String, String> properties;
    private Map<String, ? extends List<XmlInformation>> informationLists;

    public XmlInformation(Map<String, String> properties,
            Map<String, ? extends List<XmlInformation>> informationLists) {
        if (properties != null)
            this.properties = properties;
        else
            this.properties = new HashMap<String, String>();

        if (informationLists != null)
            this.informationLists = informationLists;
        else
            this.informationLists = new HashMap<String, List<XmlInformation>>();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, ? extends List<XmlInformation>> getInformationLists() {
        return informationLists;
    }
}