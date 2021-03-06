package org.icatproject.icat_oaipmh;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Set;

public class IcatQueryParameters {

    private static int maxResults;
    private static String identifierPrefix;
    private static String icatDateTimeFormat;
    private static String icatDateTimeZone;

    private String metadataPrefix;
    private String lastDataConfiguration;
    private String lastId;
    private String from;
    private String until;
    private String fromTime;
    private String untilTime;
    private String identifierDataConfiguration;
    private String identifierId;
    private String set;

    public IcatQueryParameters(String metadataPrefix, String from, String until, String set, String uniqueIdentifier,
            Set<String> dataConfigurations) throws IllegalArgumentException, IllegalStateException, DateTimeException {
        this.metadataPrefix = metadataPrefix;

        this.lastDataConfiguration = null;
        this.lastId = null;

        this.from = from;
        this.until = until;
        this.setFromUntilTimes();

        this.set = set;

        if (uniqueIdentifier != null) {
            String[] identifierParts = uniqueIdentifier.split(":");

            if (identifierParts.length != 3)
                throw new IllegalArgumentException();

            if (!identifierParts[0].equals("oai") || !identifierParts[1].equals(identifierPrefix))
                throw new IllegalArgumentException();

            IcatQueryIdentifier identifier = parseIdentifier(identifierParts[2]);
            this.identifierDataConfiguration = identifier.getDataConfiguration();
            this.identifierId = identifier.getId();

            if (!dataConfigurations.contains(this.identifierDataConfiguration))
                throw new IllegalArgumentException();
        }
    }

    public IcatQueryParameters(String resumptionToken, Set<String> dataConfigurations)
            throws IllegalArgumentException, IllegalStateException, DateTimeException {
        String[] token = resumptionToken.split(",");

        if (token.length != 5)
            throw new IllegalArgumentException();

        this.metadataPrefix = token[0];

        IcatQueryIdentifier last = parseIdentifier(token[1]);
        this.lastDataConfiguration = last.getDataConfiguration();
        this.lastId = last.getId();

        if (!dataConfigurations.contains(this.lastDataConfiguration))
            throw new IllegalArgumentException();

        this.from = token[2].equals("null") ? null : token[2];
        this.until = token[3].equals("null") ? null : token[3];
        this.setFromUntilTimes();

        this.set = token[4].equals("null") ? null : token[4];

        this.identifierDataConfiguration = null;
        this.identifierId = null;
    }

    private void setFromUntilTimes() throws IllegalStateException, DateTimeException {
        DateTimeFormatter dtf = null;
        OffsetDateTime dtFrom = null;
        OffsetDateTime dtUntil = null;

        try {
            dtf = DateTimeFormatter.ofPattern(icatDateTimeFormat).withZone(ZoneId.of(icatDateTimeZone));
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new IllegalStateException(e.getMessage(), e.getCause());
        }

        if (this.from != null) {
            DateTimeFormatter parser = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd['T'HH:mm:ssz]")
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0).parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0).parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
                    .parseDefaulting(ChronoField.OFFSET_SECONDS, 0).toFormatter();
            dtFrom = OffsetDateTime.parse(from, parser);
            this.fromTime = dtFrom.format(dtf);
        }
        if (this.until != null) {
            DateTimeFormatter parser = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd['T'HH:mm:ssz]")
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 23).parseDefaulting(ChronoField.MINUTE_OF_HOUR, 59)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 59)
                    .parseDefaulting(ChronoField.NANO_OF_SECOND, 999999999)
                    .parseDefaulting(ChronoField.OFFSET_SECONDS, 0).toFormatter();
            dtUntil = OffsetDateTime.parse(until, parser);
            this.untilTime = dtUntil.format(dtf);
        }

        if (dtFrom != null && dtUntil != null) {
            if (dtFrom.compareTo(dtUntil) > 0) {
                throw new DateTimeException("from > until");
            }

            int testCount = 0;
            DateTimeFormatter testParser = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            try {
                testParser.parse(from);
            } catch (DateTimeException e) {
                testCount++;
            }
            try {
                testParser.parse(until);
            } catch (DateTimeException e) {
                testCount++;
            }
            if (testCount % 2 != 0) {
                throw new DateTimeException("format mismatch");
            }
        }
    }

    private IcatQueryIdentifier parseIdentifier(String identifier) throws IllegalArgumentException {
        String[] identifierItemPart = identifier.split("/");

        try {
            return new IcatQueryIdentifier(identifierItemPart[0], identifierItemPart[1]);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException();
        }
    }

    public String makeResumptionToken(String newLastDataConfiguration, String newLastId) {
        String lastIdentifier = String.format("%s/%s", newLastDataConfiguration, newLastId);
        if (until == null)
            until = formatDateTime(OffsetDateTime.now().withNano(0));
        return String.join(",", metadataPrefix, lastIdentifier, from, until, set);
    }

    public String makeWhereCondition(String dataConfiguration, String setCondition) {
        ArrayList<String> constraints = new ArrayList<String>();
        if (lastId != null && dataConfiguration.equals(lastDataConfiguration))
            constraints.add(String.format("a.id > %s", lastId));
        if (fromTime != null)
            constraints.add(String.format("a.modTime >= '%s'", fromTime));
        if (untilTime != null)
            constraints.add(String.format("a.modTime <= '%s'", untilTime));
        if (identifierId != null)
            constraints.add(String.format("a.id = %s", identifierId));
        if (setCondition != null)
            constraints.add(setCondition);

        return constraints.isEmpty() ? "" : String.format("WHERE %s", String.join(" AND ", constraints));
    }

    public static String makeUniqueIdentifier(String dataConfiguration, String id) {
        return String.format("oai:%s:%s/%s", identifierPrefix, dataConfiguration, id);
    }

    public static String makeFormattedDateTime(String dateTime) throws DateTimeException {
        if (dateTime != null)
            return formatDateTime(OffsetDateTime.parse(dateTime));
        return null;
    }

    private static String formatDateTime(OffsetDateTime dateTime) throws DateTimeException {
        return dateTime.format(DateTimeFormatter.ISO_INSTANT);
    }

    public static void setMaxResults(int maxResults) {
        IcatQueryParameters.maxResults = maxResults;
    }

    public static void setIdentifierPrefix(String identifierPrefix) {
        IcatQueryParameters.identifierPrefix = identifierPrefix;
    }

    public static void setIcatDateTimeFormat(String icatDateTimeFormat) {
        IcatQueryParameters.icatDateTimeFormat = icatDateTimeFormat;
    }

    public static void setIcatDateTimeZone(String icatDateTimeZone) {
        IcatQueryParameters.icatDateTimeZone = icatDateTimeZone;
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

    public String getSet() {
        return set;
    }
}