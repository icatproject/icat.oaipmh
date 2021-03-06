package org.icatproject.icat_oaipmh;

import java.util.HashMap;

public class ItemSet {

    private String setName;
    private HashMap<String, String> dataConfigurationsConditions;
    private HashMap<String, String> dataConfigurationsJoins;

    public ItemSet(String setName) {
        this.setName = setName;
        this.dataConfigurationsConditions = new HashMap<String, String>();
        this.dataConfigurationsJoins = new HashMap<String, String>();
    }

    public void addDataConfigurationCondition(String dataConfigurationIdentifier, String condition) {
        this.dataConfigurationsConditions.put(dataConfigurationIdentifier, condition);
    }

    public void addDataConfigurationJoin(String dataConfigurationIdentifier, String join) {
        this.dataConfigurationsJoins.put(dataConfigurationIdentifier, join);
    }

    public String getSetName() {
        return setName;
    }

    public HashMap<String, String> getDataConfigurationsConditions() {
        return dataConfigurationsConditions;
    }

    public HashMap<String, String> getDataConfigurationsJoins() {
        return dataConfigurationsJoins;
    }
}