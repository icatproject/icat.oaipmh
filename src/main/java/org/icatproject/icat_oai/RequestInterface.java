package org.icatproject.icat_oai;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.transform.TransformerConfigurationException;

import org.icatproject.icat_oai.exceptions.InternalException;
import org.icatproject.utils.CheckedProperties;
import org.icatproject.utils.CheckedProperties.CheckedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@Path("/")
@Stateless
public class RequestInterface {

	private static final Logger logger = LoggerFactory.getLogger(RequestInterface.class);
	private static final Marker fatal = MarkerFactory.getMarker("FATAL");

	private static final String[] propertyTypes = { "stringProperties", "numericProperties", "dateProperties" };

	RequestHandler bean;

	@PostConstruct
	private void init() {
		CheckedProperties props = new CheckedProperties();
		try {
			props.loadFromResource("run.properties");

			String icatUrl = props.getString("icat.url");
			String[] icatAuth = props.getString("icat.auth").split("\\s+");

			String[] adminEmails = props.getString("adminEmails").split("\\s+");

			boolean debug = false;
			if (props.has("debug") && props.getString("debug").equals("true"))
				debug = true;

			String dataPrefix = String.format("data");
			String propName;

			propName = String.format("%s.mainObject", dataPrefix);
			String mainObject = props.getString(propName);

			propName = String.format("%s.includedObjects", dataPrefix);
			String includedObjects = props.getString(propName);

			propName = String.format("%s.deletedIfAllNull", dataPrefix);
			String[] deletedIfAllNullList = props.getString(propName).split("\\.");
			ArrayList<String> deletedIfAllNull = new ArrayList<String>();
			deletedIfAllNull.add(mainObject);
			for (String obj : deletedIfAllNullList)
				deletedIfAllNull.add(obj);

			RequestedProperties requestedProperties = getRequestedProperties(props, dataPrefix, mainObject);

			DataConfiguration dataConfiguration = new DataConfiguration(mainObject, includedObjects, deletedIfAllNull,
					requestedProperties);

			bean = new RequestHandler(icatUrl, icatAuth, adminEmails, dataConfiguration, debug);

			String[] prefixes = props.getString("metadataPrefixes").split("\\s+");
			for (String prefix : prefixes) {
				MetadataFormat format = new MetadataFormat(prefix, props.getString(prefix + ".xslt"),
						props.getString(prefix + ".namespace"), props.getString(prefix + ".schema"));
				bean.registerMetadataFormat(format);
			}
		} catch (CheckedPropertyException | FileNotFoundException | TransformerConfigurationException e) {
			logger.error(fatal, e.getMessage());
			throw new IllegalStateException(e.getMessage());
		} catch (InternalException e) {
			throw new IllegalStateException();
		}

		logger.info("Initialised RequestInterface");
	}

	private RequestedProperties getRequestedProperties(CheckedProperties props, String prefix, String object)
			throws CheckedPropertyException {
		String propsName;

		HashMap<String, ArrayList<String>> propertiesMap = new HashMap<String, ArrayList<String>>();
		for (String propertyType : propertyTypes) {
			propsName = String.format("%s.%s", prefix, propertyType);
			if (props.has(propsName)) {
				ArrayList<String> properties = new ArrayList<String>();
				String[] propsList = props.getString(propsName).split("\\s+");
				for (String prop : propsList)
					properties.add(prop);
				propertiesMap.put(propertyType, properties);
			}
		}

		ArrayList<RequestedProperties> subPropertyLists = new ArrayList<RequestedProperties>();
		propsName = String.format("%s.subPropertyLists", prefix);
		if (props.has(propsName)) {
			String[] subPropertyObjects = props.getString(propsName).split("\\s+");
			for (String subObject : subPropertyObjects) {
				String extendedPrefix = String.format("%s.%s", prefix, subObject);
				RequestedProperties requestedProperties = getRequestedProperties(props, extendedPrefix, subObject);
				subPropertyLists.add(requestedProperties);
			}
		}

		RequestedProperties requestedProperties = new RequestedProperties(object, propertiesMap.get("stringProperties"),
				propertiesMap.get("numericProperties"), propertiesMap.get("dateProperties"), subPropertyLists);

		return requestedProperties;
	}

	@GET
	@Path("version")
	@Produces(MediaType.APPLICATION_JSON)
	public String getVersion() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = Json.createGenerator(baos);
		gen.writeStartObject().write("version", Constants.API_VERSION).writeEnd();
		gen.close();
		return baos.toString();
	}

	@POST
	@Path("request")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.TEXT_XML)
	public String postRequest(@Context HttpServletRequest req) throws InternalException {
		return bean.request(req);
	}

	@GET
	@Path("request")
	@Produces(MediaType.TEXT_XML)
	public String getRequest(@Context HttpServletRequest req) throws InternalException {
		return bean.request(req);
	}
}