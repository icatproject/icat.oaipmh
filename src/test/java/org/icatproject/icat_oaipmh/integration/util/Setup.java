package org.icatproject.icat_oaipmh.integration.util;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.icatproject.icat.client.IcatException;
import org.icatproject.icat.client.Session;
import org.icatproject.icat.client.Session.Attributes;
import org.icatproject.icat.client.Session.DuplicateAction;
import org.icatproject.icat_oaipmh.ICATInterface;
import org.icatproject.utils.CheckedProperties;
import org.icatproject.utils.ShellCommand;

/*
 * Setup the test environment for icat.oaipmh.
 * Call this class before running any tests.
 */
public class Setup {

	private String requestUrl = null;
	private Path home;

	public Setup(String runPropertiesFile, String icatdumpDataFile) throws Exception {
		// Test home directory
		String testHome = System.getProperty("testHome");
		if (testHome == null) {
			testHome = System.getProperty("user.home");
		}
		home = Paths.get(testHome);

		// Read the test.properties
		Properties testProps = new Properties();
		InputStream is = Setup.class.getClassLoader().getResourceAsStream("test.properties");
		try {
			testProps.load(is);
		} catch (Exception e) {
			System.err.println("Problem loading test.properties: " + e.getClass() + " " + e.getMessage());
		}

		String containerHome = System.getProperty("containerHome");
		if (containerHome == null) {
			System.err.println("containerHome is not defined as a system property");
		}

		String icatUrl = System.getProperty("serverUrl");
		String icatAuth = testProps.getProperty("auth");

		// Prepare run.properties and deploy icat.oaipmh
		long time = System.currentTimeMillis();

		ShellCommand sc = new ShellCommand("src/test/scripts/prepare_test.py", "src/test/resources/", runPropertiesFile,
				icatdumpDataFile, home.toString(), containerHome, icatUrl, icatAuth);
		System.out.println(sc.getStdout() + " " + sc.getStderr());

		// Read some values from the run.properties
		CheckedProperties runProperties = new CheckedProperties();
		runProperties.loadFromFile("src/test/install/run.properties");
		requestUrl = runProperties.getString("requestUrl");
		String dataPath = runProperties.getString("dataPath");

		// Populate ICAT with sample data
		populateIcat(icatUrl, icatAuth, dataPath);

		System.out.println("Setup for " + runPropertiesFile + " and " + icatdumpDataFile + " took "
				+ (System.currentTimeMillis() - time) / 1000. + " seconds");
	}

	public void populateIcat(String icatUrl, String icatAuth, String dataPath)
			throws URISyntaxException, IcatException {
		String[] icatAuthCredentials = icatAuth.split("\\s+");

		ICATInterface restIcat = new ICATInterface(icatUrl);
		restIcat.login(icatAuthCredentials);

		Session session = restIcat.getIcatSession();
		Integer maxEntities = restIcat.getIcatMaxEntities();

		deleteIcatObjects(session, maxEntities, "Investigation");
		deleteIcatObjects(session, maxEntities, "Study");
		deleteIcatObjects(session, maxEntities, "User");
		deleteIcatObjects(session, maxEntities, "Facility");
		deleteIcatObjects(session, maxEntities, "DataCollection");
		deleteIcatObjects(session, maxEntities, "PublicStep");
		deleteIcatObjects(session, maxEntities, "Grouping");
		deleteIcatObjects(session, maxEntities, "Rule");

		Path path = Paths.get(dataPath);
		DuplicateAction duplicateAction = DuplicateAction.OVERWRITE;
		Attributes attributes = Attributes.ALL;

		session.importMetaData(path, duplicateAction, attributes);
	}

	public void deleteIcatObjects(Session session, Integer maxEntities, String object) throws IcatException {
		JsonReader jsonReader;
		JsonArray jsonArray;
		Integer offset = Integer.valueOf(0);
		do {
			String results = session
					.search(String.format("SELECT a.id FROM %s a LIMIT %s,%s", object, offset, maxEntities));
			offset += maxEntities;

			jsonReader = Json.createReader(new StringReader(results));
			jsonArray = jsonReader.readArray();
			jsonReader.close();

			for (JsonValue id : jsonArray) {
				session.delete(String.format("{\"%s\":{\"id\":%s}}", object, id));
			}
		} while (jsonArray.size() == maxEntities);
	}

	public String getRequestUrl() {
		return requestUrl;
	}
}
