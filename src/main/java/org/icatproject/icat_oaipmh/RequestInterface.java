package org.icatproject.icat_oaipmh;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import javax.xml.transform.TransformerConfigurationException;

import org.icatproject.icat_oaipmh.exceptions.InternalException;
import org.icatproject.utils.CheckedProperties;
import org.icatproject.utils.CheckedProperties.CheckedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
@Stateless
public class RequestInterface {

	private static final Logger logger = LoggerFactory.getLogger(RequestInterface.class);

	private static final String[] propertyTypes = { "stringProperties", "numericProperties", "dateProperties" };

	RequestHandler bean;

	@PostConstruct
	private void init() {
		CheckedProperties props = new CheckedProperties();
		try {
			props.loadFromResource("run.properties");

			String icatUrl = props.getString("icat.url");
			String[] icatAuth = props.getString("icat.auth").split("\\s+");

			String repositoryName = props.getString("repositoryName");
			String[] adminEmails = props.getString("adminEmails").split("\\s+");
			String requestUrl = props.getString("requestUrl");

			String maxResultsString = props.getString("maxResults");
			int maxResults = Integer.parseInt(maxResultsString);

			URI requestUri = new URI(requestUrl);
			String identifierPrefix = requestUri.getHost();

			String icatDateTimeFormat = props.getString("icatDateTimeFormat");
			String icatDateTimeZone = props.getString("icatDateTimeZone");

			IcatQueryParameters.setMaxResults(maxResults);
			IcatQueryParameters.setIdentifierPrefix(identifierPrefix);
			IcatQueryParameters.setIcatDateTimeFormat(icatDateTimeFormat);
			IcatQueryParameters.setIcatDateTimeZone(icatDateTimeZone);

			boolean responseDebug = false;
			if (props.has("responseDebug") && props.getString("responseDebug").equals("true"))
				responseDebug = true;

			String responseStyle = null;
			if (props.has("responseStyle"))
				responseStyle = props.getString("responseStyle");

			bean = new RequestHandler(icatUrl, icatAuth, repositoryName, adminEmails, requestUrl, responseDebug,
					responseStyle);

			String[] metadataFormats = props.getString("metadataPrefixes").split("\\s+");
			for (String identifier : metadataFormats) {
				MetadataFormat metadataFormat = new MetadataFormat(props.getString(identifier + ".xslt"),
						props.getString(identifier + ".namespace"), props.getString(identifier + ".schema"),
						responseDebug);
				bean.registerMetadataFormat(identifier, metadataFormat);
			}

			String dataPrefix, propName;
			String[] dataConfigurations = props.getString("data.configurations").split("\\s+");
			for (String identifier : dataConfigurations) {
				dataPrefix = String.format("data.%s", identifier);

				propName = String.format("%s.metadataPrefixes", dataPrefix);
				String[] metadataPrefixes = props.getString(propName).split("\\s+");

				propName = String.format("%s.mainObject", dataPrefix);
				String mainObject = props.getString(propName);

				RequestedProperties requestedProperties = getRequestedProperties(props, dataPrefix, mainObject);

				DataConfiguration dataConfiguration = new DataConfiguration(metadataPrefixes, mainObject,
						requestedProperties);
				bean.registerDataConfiguration(identifier, dataConfiguration);
			}

			if (props.has("sets")) {
				String[] sets = props.getString("sets").split("\\s+");
				for (String setSpec : sets) {
					propName = String.format("sets.%s.name", setSpec);
					String setName = props.getString(propName);

					ItemSet set = new ItemSet(setName);

					propName = String.format("sets.%s.configurations", setSpec);
					String[] setDataConfigurations = props.getString(propName).split("\\s+");
					for (String setDataConfiguration : setDataConfigurations) {
						String condition = null;
						String join = null;
						propName = String.format("sets.%s.condition.%s", setSpec, setDataConfiguration);
						if (props.has(propName)) {
							condition = props.getString(propName);
						}
						propName = String.format("sets.%s.join.%s", setSpec, setDataConfiguration);
						if (props.has(propName)) {
							join = props.getString(propName);
						}
						set.addDataConfigurationCondition(setDataConfiguration, condition);
						set.addDataConfigurationJoin(setDataConfiguration, join);
					}
					bean.registerSet(setSpec, set);
				}
			}
		} catch (CheckedPropertyException | FileNotFoundException | SecurityException | NumberFormatException
				| TransformerConfigurationException | URISyntaxException e) {
			logger.error(e.getMessage());
			throw new IllegalStateException("Configuration exception during initialization", e);
		} catch (InternalException e) {
			throw new IllegalStateException("Internal exception during initialization", e);
		}

		logger.info("Initialized RequestInterface");
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
