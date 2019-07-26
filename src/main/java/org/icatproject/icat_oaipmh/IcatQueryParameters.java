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
    private String lastDataConfiguration;
    private String lastId;
    private String from;
    private String until;
    private String fromTime;
    private String untilTime;
    private String identifierDataConfiguration;
    private String identifierId;

    public IcatQueryParameters(String metadataPrefix, String from, String until, String uniqueIdentifier,
            Set<String> dataConfigurations) throws InternalException {
        this.metadataPrefix = metadataPrefix;

        this.lastDataConfiguration = null;
        this.lastId = null;

        this.from = from;
        this.until = until;
        this.setFromUntilTimes();

        if (uniqueIdentifier != null) {
            String[] identifierParts = uniqueIdentifier.split(":");

            if (identifierParts.length != 3)
                throw new InternalException();

            if (!identifierParts[0].equals("oai") || !identifierParts[1].equals(identifierPrefix))
                throw new InternalException();

            IcatQueryIdentifier identifier = parseIdentifier(identifierParts[2]);
            this.identifierDataConfiguration = identifier.getDataConfiguration();
            this.identifierId = identifier.getId();

            if (!dataConfigurations.contains(this.identifierDataConfiguration))
                throw new InternalException();
        }
    }

    public IcatQueryParameters(String resumptionToken, Set<String> dataConfigurations) throws InternalException {
        String[] token = resumptionToken.split(",");

        if (token.length != 4)
            throw new InternalException();

        this.metadataPrefix = token[0];

        IcatQueryIdentifier last = parseIdentifier(token[1]);
        this.lastDataConfiguration = last.getDataConfiguration();
        this.lastId = last.getId();

        if (!dataConfigurations.contains(this.lastDataConfiguration))
            throw new InternalException();

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

    private IcatQueryIdentifier parseIdentifier(String identifier) throws InternalException {
        String[] identifierItemPart = identifier.split("/");

        try {
            return new IcatQueryIdentifier(identifierItemPart[0], identifierItemPart[1]);
        } catch (IndexOutOfBoundsException e) {
            throw new InternalException();
        }
    }

    public String makeResumptionToken(String newLastDataConfiguration, String newLastId) {
        String lastIdentifier = String.format("%s/%s", newLastDataConfiguration, newLastId);
        if (until == null)
            until = formatDateTime(OffsetDateTime.now().withNano(0));
        return String.join(",", metadataPrefix, lastIdentifier, from, until);
    }

    public String makeWhereCondition(String dataConfiguration) {
        ArrayList<String> constraints = new ArrayList<String>();
        if (lastId != null && dataConfiguration.equals(lastDataConfiguration))
            constraints.add(String.format("a.id > %s", lastId));
        if (fromTime != null)
            constraints.add(String.format("a.modTime >= '%s'", fromTime));
        if (untilTime != null)
            constraints.add(String.format("a.modTime <= '%s'", untilTime));
        if (identifierId != null)
            constraints.add(String.format("a.id = %s", identifierId));

        return constraints.isEmpty() ? "" : String.format("WHERE %s", String.join(" AND ", constraints));
    }

    public static String makeUniqueIdentifier(String dataConfiguration, String id) {
        return String.format("oai:%s:%s/%s", identifierPrefix, dataConfiguration, id);
    }

    public static String makeFormattedDateTime(String dateTime) {
        if (dateTime != null)
            return formatDateTime(OffsetDateTime.parse(dateTime));
        return null;
    }

    private static String formatDateTime(OffsetDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_INSTANT);
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

    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    public String getLastDataConfiguration() {
        return lastDataConfiguration;
    }

    public String getIdentifierDataConfiguration() {
        return identifierDataConfiguration;
    }
}