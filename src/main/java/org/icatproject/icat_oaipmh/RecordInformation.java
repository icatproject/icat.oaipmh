package org.icatproject.icat_oaipmh;

public class RecordInformation {

    private XmlInformation header;
    private XmlInformation metadata;

    public RecordInformation(XmlInformation header, XmlInformation metadata) {
        this.header = header;
        this.metadata = metadata;
    }

    public XmlInformation getHeader() {
        return header;
    }

    public XmlInformation getMetadata() {
        return metadata;
    }
}