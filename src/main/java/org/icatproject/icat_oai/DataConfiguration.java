package org.icatproject.icat_oai;

import java.util.List;

public class DataConfiguration {

    private String mainObject;
    private String includedObjects;
    private List<String> deletedIfAllNull;
    private RequestedProperties requestedProperties;

    public DataConfiguration(String mainObject, String includedObjects, List<String> deletedIfAllNull,
            RequestedProperties requestedProperties) {
        this.mainObject = mainObject;
        this.includedObjects = includedObjects;
        this.deletedIfAllNull = deletedIfAllNull;
        this.requestedProperties = requestedProperties;
    }

    public String getMainObject() {
        return mainObject;
    }

    public String getIncludedObjects() {
        return includedObjects;
    }

    public List<String> getDeletedIfAllNull() {
        return deletedIfAllNull;
    }

    public RequestedProperties getRequestedProperties() {
        return requestedProperties;
    }
}