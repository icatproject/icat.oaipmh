package org.icatproject.icat_oaipmh;

import java.util.ArrayList;
import java.util.List;

public class IcatQueryResults {

    private IcatQuery query;
    private List<RecordInformation> results;

    public IcatQueryResults(IcatQuery query, List<RecordInformation> results) {
        this.query = query;

        if (results != null)
            this.results = results;
        else
            this.results = new ArrayList<RecordInformation>();
    }

    public List<RecordInformation> getResults() {
        return results;
    }

    public boolean getIncomplete() {
        return query.getIncomplete();
    }

    public int getSize() {
        return query.getSize();
    }

    public int getCursor() {
        return query.getCursor();
    }
}