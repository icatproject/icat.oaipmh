package org.icatproject.icat_oaipmh;

import java.util.ArrayList;
import java.util.List;

public class IcatQueryResults {

    private List<RecordInformation> results;
    private boolean incomplete;

    public IcatQueryResults(List<RecordInformation> results, boolean incomplete) {
        this.incomplete = incomplete;

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
}