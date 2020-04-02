package org.icatproject.icat_oaipmh;

import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.icatproject.icat.client.ICAT;
import org.icatproject.icat.client.IcatException;
import org.icatproject.icat.client.Session;

public class ICATInterface {

    private ICAT icat;
    private Session icatSession;
    private Integer icatMaxEntities;

    public ICATInterface(String icatUrl) throws URISyntaxException, IcatException {
        icat = new ICAT(icatUrl);

        String icatPropertiesString = icat.getProperties();
        JsonReader jsonReader = Json.createReader(new StringReader(icatPropertiesString));
        JsonObject icatProperties = jsonReader.readObject();
        jsonReader.close();

        icatMaxEntities = Integer.valueOf(icatProperties.getInt("maxEntities"));
    }

    public void login(String[] icatAuth) throws IcatException {
        HashMap<String, String> credentials = new HashMap<String, String>();
        for (int i = 1; i < icatAuth.length; i += 2) {
            credentials.put(icatAuth[i], icatAuth[i + 1]);
        }

        icatSession = icat.login(icatAuth[0], credentials);
    }

    public Session getIcatSession() {
        return icatSession;
    }

    public Integer getIcatMaxEntities() {
        return icatMaxEntities;
    }
}