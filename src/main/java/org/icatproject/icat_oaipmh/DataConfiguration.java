package org.icatproject.icat_oaipmh;

public class DataConfiguration {

    private String mainObject;
    private String includedObjects;
    private RequestedProperties requestedProperties;

    public DataConfiguration(String mainObject, String includedObjects, RequestedProperties requestedProperties) {
        this.mainObject = mainObject;
        this.includedObjects = includedObjects;
        this.requestedProperties = requestedProperties;
    }

    public String getMainObject() {
        return mainObject;
    }

    public String getIncludedObjects() {
        return includedObjects;
    }

    public RequestedProperties getRequestedProperties() {
        return requestedProperties;
    }
}