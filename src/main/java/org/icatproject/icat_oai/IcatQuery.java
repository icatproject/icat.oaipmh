package org.icatproject.icat_oai;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonValue;

public class IcatQuery {

    private List<JsonValue> results;
    private boolean incomplete;

    public IcatQuery(List<JsonValue> results, boolean incomplete) {
        this.incomplete = incomplete;

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
}