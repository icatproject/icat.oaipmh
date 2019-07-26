package org.icatproject.icat_oaipmh;

public class IcatQueryIdentifier {

    private String dataConfiguration;
    private String id;

    public IcatQueryIdentifier(String dataConfiguration, String id) {
        this.dataConfiguration = dataConfiguration;
        this.id = id;
    }

    public String getDataConfiguration() {
        return dataConfiguration;
    }

    public String getId() {
        return id;
    }
}