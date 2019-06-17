package org.icatproject.icat_oai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlInformation {

    private Map<String, String> singleProperties;
    private Map<String, ? extends List<String>> repeatedProperties;
    private Map<String, ? extends List<XmlInformation>> informationLists;

    public XmlInformation(Map<String, String> singleProperties, Map<String, ? extends List<String>> repeatedProperties,
            Map<String, ? extends List<XmlInformation>> informationLists) {
        if (singleProperties != null)
            this.singleProperties = singleProperties;
        else
            this.singleProperties = new HashMap<String, String>();

        if (repeatedProperties != null)
            this.repeatedProperties = repeatedProperties;
        else
            this.repeatedProperties = new HashMap<String, List<String>>();

        if (informationLists != null)
            this.informationLists = informationLists;
        else
            this.informationLists = new HashMap<String, List<XmlInformation>>();
    }

    public Map<String, String> getSingleProperties() {
        return singleProperties;
    }

    public Map<String, ? extends List<String>> getRepeatedProperties() {
        return repeatedProperties;
    }

    public Map<String, ? extends List<XmlInformation>> getInformationLists() {
        return informationLists;
    }
}