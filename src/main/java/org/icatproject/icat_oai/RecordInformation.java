package org.icatproject.icat_oai;

public class RecordInformation {

    private HeaderInformation header;
    private MetadataInformation metadata;

    public RecordInformation(HeaderInformation header, MetadataInformation metadata) {
        this.header = header;
        this.metadata = metadata;
    }

    public HeaderInformation getHeader() {
        return header;
    }

    public MetadataInformation getMetadata() {
        return metadata;
    }
}