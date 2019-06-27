package org.icatproject.icat_oaipmh;

import java.util.ArrayList;
import java.util.List;

public class RequestedProperties {

    private String icatObject;
    private List<String> stringProperties;
    private List<String> numericProperties;
    private List<String> dateProperties;
    private List<RequestedProperties> subPropertyLists;

    public RequestedProperties(String icatObject, List<String> stringProperties, List<String> numericProperties,
            List<String> dateProperties, List<RequestedProperties> subPropertyLists) {
        this.icatObject = icatObject;

        if (stringProperties != null)
            this.stringProperties = stringProperties;
        else
            this.stringProperties = new ArrayList<String>();

        if (numericProperties != null)
            this.numericProperties = numericProperties;
        else
            this.numericProperties = new ArrayList<String>();

        if (dateProperties != null)
            this.dateProperties = dateProperties;
        else
            this.dateProperties = new ArrayList<String>();

        if (subPropertyLists != null)
            this.subPropertyLists = subPropertyLists;
        else
            this.subPropertyLists = new ArrayList<RequestedProperties>();
    }

    public String getIcatObject() {
        return icatObject;
    }

    public List<String> getStringProperties() {
        return stringProperties;
    }

    public List<String> getNumericProperties() {
        return numericProperties;
    }

    public List<String> getDateProperties() {
        return dateProperties;
    }

    public List<RequestedProperties> getSubPropertyLists() {
        return subPropertyLists;
    }
}