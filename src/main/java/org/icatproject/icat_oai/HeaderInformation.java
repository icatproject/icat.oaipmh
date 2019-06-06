package org.icatproject.icat_oai;

public class HeaderInformation {

    private String identifier;
    private String datestamp;
    private boolean deleted;

    public HeaderInformation(String identifier, String datestamp, boolean deleted) {
        this.identifier = identifier;
        this.datestamp = datestamp;
        this.deleted = deleted;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDatestamp() {
        return datestamp;
    }

    public boolean getDeleted() {
        return deleted;
    }
}