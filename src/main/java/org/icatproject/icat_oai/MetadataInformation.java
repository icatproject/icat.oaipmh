package org.icatproject.icat_oai;

import java.util.ArrayList;

public class MetadataInformation {

    private String doi;
    private ArrayList<MetadataUser> investigationUsers;
    private String title;
    private String releaseDate;
    private String startDate;
    private String endDate;
    private String name;
    private String visitId;
    private String summary;

    public MetadataInformation(String doi, ArrayList<MetadataUser> investigationUsers, String title, String releaseDate,
            String startDate, String endDate, String name, String visitId, String summary) {
        this.doi = doi;
        this.investigationUsers = investigationUsers;
        this.title = title;
        this.releaseDate = releaseDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
        this.visitId = visitId;
        this.summary = summary;
    }

    public String getDoi() {
        return doi;
    }

    public ArrayList<MetadataUser> getInvestigationUsers() {
        return investigationUsers;
    }

    public String getTitle() {
        return title;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getName() {
        return name;
    }

    public String getVisitId() {
        return visitId;
    }

    public String getSummary() {
        return summary;
    }
}