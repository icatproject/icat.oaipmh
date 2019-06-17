package org.icatproject.icat_oai;

import java.util.ArrayList;
import java.util.List;

public class DataConfiguration {

    private String mainObject;
    private List<String> includedObjects;
    private List<String> deletedIfAllNull;
    private RequestedProperties requestedProperties;

    public DataConfiguration(String mainObject, List<String> includedObjects, List<String> deletedIfAllNull,
            RequestedProperties requestedProperties) {
        this.mainObject = mainObject;

        if (includedObjects != null)
            this.includedObjects = includedObjects;
        else
            this.includedObjects = new ArrayList<String>();

        this.deletedIfAllNull = deletedIfAllNull;

        this.requestedProperties = requestedProperties;
    }

    public String getMainObject() {
        return mainObject;
    }

    public List<String> getIncludedObjects() {
        return includedObjects;
    }

    public List<String> getDeletedIfAllNull() {
        return deletedIfAllNull;
    }

    public RequestedProperties getRequestedProperties() {
        return requestedProperties;
    }
}