package org.icatproject.icat_oaipmh.integration.util;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.icatproject.icat.client.ICAT;
import org.icatproject.icat.client.IcatException;
import org.icatproject.icat.client.Session;
import org.icatproject.icat.client.Session.Attributes;
import org.icatproject.icat.client.Session.DuplicateAction;
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

		ShellCommand sc = new ShellCommand("src/test/scripts/prepare_test.py", "src/test/resources/",
				runPropertiesFile, icatdumpDataFile, home.toString(), containerHome, icatUrl, icatAuth);
		System.out.println(sc.getStdout() + " " + sc.getStderr());

		// Read some values from the run.properties
		CheckedProperties runProperties = new CheckedProperties();
		runProperties.loadFromFile("src/test/install/run.properties");
		requestUrl = runProperties.getString("requestUrl");
		String dataPath = runProperties.getString("dataPath");

		// If the configuration has changed, populate ICAT with sample data
		if (sc.getExitValue() != 8)
			populateIcat(icatUrl, icatAuth, dataPath);

		System.out.println("Setup for " + runPropertiesFile + " and " + icatdumpDataFile + " took "
				+ (System.currentTimeMillis() - time) / 1000. + " seconds");
	}

	public void populateIcat(String icatUrl, String icatAuth, String dataPath)
			throws URISyntaxException, IcatException {
		ICAT icat = new ICAT(icatUrl);

		String[] icatAuthCredentials = icatAuth.split("\\s+");
		HashMap<String, String> credentials = new HashMap<String, String>();
		for (int i = 1; i < icatAuthCredentials.length; i += 2) {
			credentials.put(icatAuthCredentials[i], icatAuthCredentials[i + 1]);
		}

		Session session = icat.login(icatAuthCredentials[0], credentials);

		deleteIcatObjects(session, "Investigation");
		deleteIcatObjects(session, "Study");
		deleteIcatObjects(session, "User");
		deleteIcatObjects(session, "Facility");
		deleteIcatObjects(session, "DataCollection");
		deleteIcatObjects(session, "PublicStep");
		deleteIcatObjects(session, "Grouping");
		deleteIcatObjects(session, "Rule");

		Path path = Paths.get(dataPath);
		DuplicateAction duplicateAction = DuplicateAction.OVERWRITE;
		Attributes attributes = Attributes.ALL;

		session.importMetaData(path, duplicateAction, attributes);
	}

	public void deleteIcatObjects(Session session, String object) throws IcatException {
		String results = session.search(String.format("%s.id", object));

		JsonReader jsonReader = Json.createReader(new StringReader(results));
		JsonArray jsonArray = jsonReader.readArray();
		jsonReader.close();

		for (JsonValue id : jsonArray) {
			session.delete(String.format("{\"%s\":{\"id\":%s}}", object, id));
		}
	}

	public String getRequestUrl() {
		return requestUrl;
	}
}
