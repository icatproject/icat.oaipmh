package org.icatproject.icat_oaipmh;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonValue;

public class IcatQuery {

    private List<JsonValue> results;
    private boolean incomplete;
    private int size;
    private int cursor;

    public IcatQuery(List<JsonValue> results, boolean incomplete, int size, int cursor) {
        this.incomplete = incomplete;
        this.size = size;
        this.cursor = cursor;

        if (results != null)
            this.results = results;
        else
            this.results = new ArrayList<JsonValue>();
    }

    public List<JsonValue> getResults() {
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