package org.icatproject.icat_oaipmh;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.icatproject.icat_oaipmh.exceptions.InternalException;

public class IcatQueryParameters {

    private static int maxResults;
    private static String identifierPrefix;

    private int offset;
    private String from;
    private String until;
    private String fromTime;
    private String untilTime;
    private String identifierId;

    public IcatQueryParameters(int offset, String from, String until, String identifier) throws InternalException {
        this.offset = offset;

        this.from = from;
        this.until = until;
        this.setFromUntilTimes();

        if (identifier != null) {
            String[] identifierParts = identifier.split(":");
            String schema, namespace, id;

            try {
                schema = identifierParts[0];
                namespace = identifierParts[1];
                id = identifierParts[2];
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new InternalException();
            }

            if (!schema.equals("oai") || !namespace.equals(identifierPrefix))
                throw new InternalException();
            this.identifierId = id;
        }
    }

    public IcatQueryParameters(String resumptionToken) {
        String[] token = resumptionToken.split(",");

        this.offset = Integer.parseInt(token[1]);

        this.from = token[2].equals("null") ? null : token[2];
        this.until = token[3].equals("null") ? null : token[3];
        this.setFromUntilTimes();

        this.identifierId = null;
    }

    private void setFromUntilTimes() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        OffsetDateTime dtFrom = null;
        OffsetDateTime dtUntil = null;

        if (this.from != null) {
            dtFrom = OffsetDateTime.parse(from);
            this.fromTime = dtFrom.format(dtf);
        }
        if (this.until != null) {
            dtUntil = OffsetDateTime.parse(until);
            this.untilTime = dtUntil.format(dtf);
        }

        if (dtFrom != null && dtUntil != null) {
            if (dtFrom.compareTo(dtUntil) > 0) {
                throw new IllegalArgumentException();
            }
        }
    }

    public String makeResumptionToken(String metadataPrefix) {
        Integer offset = this.offset + maxResults;
        return String.join(",", metadataPrefix, offset.toString(), from, until);
    }

    public String makeWhereCondition() {
        ArrayList<String> constraints = new ArrayList<String>();
        if (fromTime != null) {
            constraints.add(String.format("a.modTime >= '%s'", fromTime));
        }
        if (untilTime != null) {
            constraints.add(String.format("a.modTime <= '%s'", untilTime));
        }
        if (identifierId != null)
            constraints.add(String.format("a.id = %s", identifierId));

        return constraints.isEmpty() ? "" : String.format("WHERE %s", String.join(" AND ", constraints));
    }

    public static void setMaxResults(int maxResults) {
        IcatQueryParameters.maxResults = maxResults;
    }

    public static void setIdentifierPrefix(String identifierPrefix) {
        IcatQueryParameters.identifierPrefix = identifierPrefix;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public String getIdentifierPrefix() {
        return identifierPrefix;
    }

    public int getOffset() {
        return offset;
    }
}