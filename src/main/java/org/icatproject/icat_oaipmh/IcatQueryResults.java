package org.icatproject.icat_oaipmh;

import java.util.ArrayList;
import java.util.List;

public class IcatQueryResults {

    private List<RecordInformation> results;
    private boolean incomplete;
    private String lastDataConfiguration;
    private String lastId;

    public IcatQueryResults(List<RecordInformation> results, boolean incomplete, String lastDataConfiguration,
            String lastId) {
        this.incomplete = incomplete;
        this.lastDataConfiguration = lastDataConfiguration;
        this.lastId = lastId;

        if (results != null)
            this.results = results;
        else
            this.results = new ArrayList<RecordInformation>();
    }

    public List<RecordInformation> getResults() {
        return results;
    }

    public boolean getIncomplete() {
        return incomplete;
    }

    public String getLastDataConfiguration() {
        return lastDataConfiguration;
    }

    public String getLastId() {
        return lastId;
    }
}