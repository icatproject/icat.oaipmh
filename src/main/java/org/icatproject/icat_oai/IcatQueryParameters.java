package org.icatproject.icat_oai;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class IcatQueryParameters {

    private static int offsetSize = 50;

    private int offset;
    private String from;
    private String until;
    private String identifierId;
    private String identifierPrefix;

    public IcatQueryParameters(int offset, String from, String until, String identifier, String identifierPrefix) {
        this.offset = offset;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        if (from != null)
            this.from = OffsetDateTime.parse(from).format(dtf);
        if (until != null)
            this.until = OffsetDateTime.parse(until).format(dtf);

        if (identifier != null)
            this.identifierId = identifier.split(":")[2];
        this.identifierPrefix = identifierPrefix;
    }

    public IcatQueryParameters(String resumptionToken, String identifierPrefix) {
        String[] token = resumptionToken.split(",");

        this.offset = Integer.parseInt(token[1]);

        this.from = token[2].equals("null") ? null : token[2];
        this.until = token[3].equals("null") ? null : token[3];

        this.identifierId = null;
        this.identifierPrefix = identifierPrefix;
    }

    public String makeResumptionToken(String metadataPrefix) {
        Integer offset = this.offset + offsetSize;
        return String.join(",", metadataPrefix, offset.toString(), from, until);
    }

    public String makeWhereCondition() {
        ArrayList<String> constraints = new ArrayList<String>();
        if (from != null) {
            constraints.add(String.format("d.modTime >= '%s'", from));
        }
        if (until != null) {
            constraints.add(String.format("d.modTime <= '%s'", until));
        }
        if (identifierId != null)
            constraints.add(String.format("d.id = %s", identifierId));

        return constraints.isEmpty() ? "" : String.format("WHERE %s", String.join(" AND ", constraints));
    }

    public int getOffsetSize() {
        return offsetSize;
    }

    public int getOffset() {
        return offset;
    }

    public String getIdentifierPrefix() {
        return identifierPrefix;
    }
}