package org.icatproject.icat_oaipmh;

import java.util.ArrayList;
import java.util.List;

public class IcatQueryResults {

    private List<RecordInformation> results;
    private boolean incomplete;
    private int size;
    private int cursor;

    public IcatQueryResults(List<RecordInformation> results, boolean incomplete, int size, int cursor) {
        this.incomplete = incomplete;
        this.size = size;
        this.cursor = cursor;

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

    public int getSize() {
        return size;
    }

    public int getCursor() {
        return cursor;
    }
}