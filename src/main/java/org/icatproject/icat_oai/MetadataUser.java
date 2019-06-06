package org.icatproject.icat_oai;

public class MetadataUser {

    private String fullName;
    private String givenName;
    private String familyName;
    private String orcidId;

    public MetadataUser(String fullName, String givenName, String familyName, String orcidId) {
        this.fullName = fullName;
        this.givenName = givenName;
        this.familyName = familyName;
        this.orcidId = orcidId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getOrcidId() {
        return orcidId;
    }
}