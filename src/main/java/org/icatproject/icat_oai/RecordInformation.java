package org.icatproject.icat_oai;

public class RecordInformation {

    private boolean deleted;
    private XmlInformation header;
    private XmlInformation metadata;

    public RecordInformation(boolean deleted, XmlInformation header, XmlInformation metadata) {
        this.deleted = deleted;
        this.header = header;
        this.metadata = metadata;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public XmlInformation getHeader() {
        return header;
    }

    public XmlInformation getMetadata() {
        return metadata;
    }
}