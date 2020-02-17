package org.icatproject.icat_oaipmh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataConfiguration {

    private ArrayList<String> metadataPrefixes;
    private String mainObject;
    private String includedObjects;
    private RequestedProperties requestedProperties;

    private Character variable;

    public DataConfiguration(String[] metadataPrefixes, String mainObject, RequestedProperties requestedProperties) {
        this.metadataPrefixes = new ArrayList<>(Arrays.asList(metadataPrefixes));
        this.mainObject = mainObject;
        this.requestedProperties = requestedProperties;

        this.variable = 'a';
        List<String> subObjectsList = extractSubObjects(requestedProperties);
        if (subObjectsList.isEmpty())
            this.includedObjects = "";
        else
            this.includedObjects = String.format("INCLUDE %s", String.join(", ", subObjectsList));
    }

    private List<String> extractSubObjects(RequestedProperties properties) {
        char currentVariable = variable;
        List<String> subList = new ArrayList<String>();
        for (RequestedProperties props : properties.getSubPropertyLists()) {
            variable++;
            subList.add(String.format("%s.%s %s", currentVariable, props.getIcatObject(), variable));
            subList.addAll(extractSubObjects(props));
        }
        return subList;
    }

    public ArrayList<String> getMetadataPrefixes() {
        return metadataPrefixes;
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