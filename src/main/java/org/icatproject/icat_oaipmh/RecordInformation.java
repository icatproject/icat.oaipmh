package org.icatproject.icat_oaipmh;

public class RecordInformation {

    private String dataConfigurationIdentifier;
    private XmlInformation header;
    private XmlInformation metadata;

    public RecordInformation(String dataConfigurationIdentifier, XmlInformation header, XmlInformation metadata) {
        this.dataConfigurationIdentifier = dataConfigurationIdentifier;
        this.header = header;
        this.metadata = metadata;
    }

    public String getDataConfigurationIdentifier() {
        return dataConfigurationIdentifier;
    }

    public XmlInformation getHeader() {
        return header;
    }

    public XmlInformation getMetadata() {
        return metadata;
    }
}