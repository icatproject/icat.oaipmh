package org.icatproject.icat_oaipmh;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.ParseException;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Set;

import org.icatproject.icat_oaipmh.exceptions.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(XmlResponse.class);

    public IcatQueryParameters(String metadataPrefix, String from, String until, String uniqueIdentifier,
            Set<String> dataConfigurations) throws ParseException, InternalException {
        this.metadataPrefix = metadataPrefix;

        this.lastDataConfiguration = null;
        this.lastId = null;

        this.from = from;
        this.until = until;
        this.setFromUntilTimes();

        if (uniqueIdentifier != null) {
            String[] identifierParts = uniqueIdentifier.split(":");

            if (identifierParts.length != 3)
                throw new ParseException(uniqueIdentifier, 0);

            if (!identifierParts[0].equals("oai") || !identifierParts[1].equals(identifierPrefix))
                throw new ParseException(uniqueIdentifier, 0);

            IcatQueryIdentifier identifier = parseIdentifier(identifierParts[2]);
            this.identifierDataConfiguration = identifier.getDataConfiguration();
            this.identifierId = identifier.getId();

            if (!dataConfigurations.contains(this.identifierDataConfiguration))
                throw new ParseException(uniqueIdentifier, 0);
        }
    }

    public IcatQueryParameters(String resumptionToken, Set<String> dataConfigurations)
            throws ParseException, InternalException {
        String[] token = resumptionToken.split(",");

        if (token.length != 4)
            throw new ParseException(resumptionToken, 0);

        this.metadataPrefix = token[0];

        IcatQueryIdentifier last = parseIdentifier(token[1]);
        this.lastDataConfiguration = last.getDataConfiguration();
        this.lastId = last.getId();

        if (!dataConfigurations.contains(this.lastDataConfiguration))
            throw new ParseException(resumptionToken, 0);

        this.from = token[2].equals("null") ? null : token[2];
        this.until = token[3].equals("null") ? null : token[3];
        this.setFromUntilTimes();

        this.identifierDataConfiguration = null;
        this.identifierId = null;
    }

    private void setFromUntilTimes() throws InternalException {
        DateTimeFormatter dtf = null;
        OffsetDateTime dtFrom = null;
        OffsetDateTime dtUntil = null;

        try {
            dtf = DateTimeFormatter.ofPattern(icatDateTimeFormat).withZone(ZoneId.of(icatDateTimeZone));
        } catch (IllegalArgumentException | DateTimeException e) {
            logger.error(e.getMessage());
            throw new InternalException();
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
                throw new IllegalArgumentException();
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
                throw new IllegalArgumentException();
            }
        }
    }

    private IcatQueryIdentifier parseIdentifier(String identifier) throws ParseException {
        String[] identifierItemPart = identifier.split("/");

        try {
            return new IcatQueryIdentifier(identifierItemPart[0], identifierItemPart[1]);
        } catch (IndexOutOfBoundsException e) {
            throw new ParseException(identifier, 0);
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
}