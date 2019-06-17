package org.icatproject.icat_oai;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

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

	RequestHandler bean;

	@PostConstruct
	private void init() {
		CheckedProperties props = new CheckedProperties();
		try {
			props.loadFromResource("run.properties");

			String icatUrl = props.getString("icat.url");
			String[] icatAuth = props.getString("icat.auth").split("\\s+");

			String[] adminEmails = props.getString("adminEmails").split("\\s+");

			// TESTDATA BEGIN
			boolean debug = false;

			ArrayList<String> stringProperties = new ArrayList<String>();
			stringProperties.add("doi");
			stringProperties.add("title");
			stringProperties.add("name");
			stringProperties.add("visitId");
			stringProperties.add("summary");

			ArrayList<String> numericProperties = new ArrayList<String>();
			numericProperties.add("id");

			ArrayList<String> dateProperties = new ArrayList<String>();
			dateProperties.add("releaseDate");
			dateProperties.add("startDate");
			dateProperties.add("endDate");

			ArrayList<RequestedProperties> subPropertyLists = new ArrayList<RequestedProperties>();
			ArrayList<String> userStringProperties = new ArrayList<String>();
			userStringProperties.add("fullName");
			userStringProperties.add("givenName");
			userStringProperties.add("familyName");
			userStringProperties.add("orcidId");
			RequestedProperties userProps = new RequestedProperties("user", userStringProperties, null, null, null);
			ArrayList<RequestedProperties> investigationUsersSubPropertyLists = new ArrayList<RequestedProperties>();
			investigationUsersSubPropertyLists.add(userProps);
			ArrayList<String> investigationUserStringProperties = new ArrayList<String>();
			investigationUserStringProperties.add("role");
			RequestedProperties investigationUsersProps = new RequestedProperties("investigationUsers",
					investigationUserStringProperties, null, null, investigationUsersSubPropertyLists);
			subPropertyLists.add(investigationUsersProps);

			ArrayList<String> datafilesStringProperties = new ArrayList<String>();
			datafilesStringProperties.add("location");
			RequestedProperties datafilesProps = new RequestedProperties("datafiles", datafilesStringProperties, null,
					null, null);
			ArrayList<RequestedProperties> datasetsSubPropertyLists = new ArrayList<RequestedProperties>();
			datasetsSubPropertyLists.add(datafilesProps);
			RequestedProperties datasetsProps = new RequestedProperties("datasets", null, null, null,
					datasetsSubPropertyLists);
			subPropertyLists.add(datasetsProps);

			RequestedProperties requestedProperties = new RequestedProperties("Investigation", stringProperties,
					numericProperties, dateProperties, subPropertyLists);

			String mainObject = "Investigation";

			ArrayList<String> includedObjects = new ArrayList<String>();
			includedObjects.add("investigationUsers.user");
			includedObjects.add("datasets.datafiles");

			ArrayList<String> deletedIfAllNull = new ArrayList<String>();
			deletedIfAllNull.add("Investigation");
			deletedIfAllNull.add("datasets");
			deletedIfAllNull.add("datafiles");
			deletedIfAllNull.add("location");

			DataConfiguration dataConfiguration = new DataConfiguration(mainObject, includedObjects, deletedIfAllNull,
					requestedProperties);
			// TESTDATA END

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