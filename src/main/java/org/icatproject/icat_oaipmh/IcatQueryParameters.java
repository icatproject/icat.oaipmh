package org.icatproject.icat_oaipmh;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;

import org.icatproject.icat_oaipmh.exceptions.InternalException;

public class IcatQueryParameters {

    private static int maxResults;
    private static String identifierPrefix;

    private String metadataPrefix;
    private int offset;
    private String from;
    private String until;
    private String fromTime;
    private String untilTime;
    private String identifierDataConfiguration;
    private String identifierId;

    public IcatQueryParameters(String metadataPrefix, int offset, String from, String until, String identifier,
            Set<String> dataConfigurations) throws InternalException {
        this.metadataPrefix = metadataPrefix;
        this.offset = offset;

        this.from = from;
        this.until = until;
        this.setFromUntilTimes();

        if (identifier != null) {
            String[] identifierParts = identifier.split(":");

            if (identifierParts.length != 3)
                throw new InternalException();

            if (!identifierParts[0].equals("oai") || !identifierParts[1].equals(identifierPrefix))
                throw new InternalException();

            String[] identifierItemPart = identifierParts[2].split("/");

            if (identifierItemPart.length != 2)
                throw new InternalException();

            if (!dataConfigurations.contains(identifierItemPart[0]))
                throw new InternalException();

            this.identifierDataConfiguration = identifierItemPart[0];
            this.identifierId = identifierItemPart[1];
        }
    }

    public IcatQueryParameters(String resumptionToken) throws InternalException {
        String[] token = resumptionToken.split(",");

        if (token.length != 4)
            throw new InternalException();

        this.metadataPrefix = token[0];
        this.offset = Integer.parseInt(token[1]);

        this.from = token[2].equals("null") ? null : token[2];
        this.until = token[3].equals("null") ? null : token[3];
        this.setFromUntilTimes();

        this.identifierDataConfiguration = null;
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

    public String makeResumptionToken() {
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

    public static String makeUniqueIdentifier(String config, String id) {
        return String.format("oai:%s:%s/%s", identifierPrefix, config, id);
    }

    public static String makeFormattedDateTime(String dateTime) {
        if (dateTime != null)
            return OffsetDateTime.parse(dateTime).format(DateTimeFormatter.ISO_INSTANT);
        return null;
    }

    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public int getOffset() {
        return offset;
    }

    public String GetIdentifierDataConfiguration() {
        return identifierDataConfiguration;
    }
}