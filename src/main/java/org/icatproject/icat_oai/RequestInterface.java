package org.icatproject.icat_oai;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;

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

			String repositoryName = props.getString("repositoryName");
			String[] adminEmails = props.getString("adminEmails").split("\\s+");

			bean = new RequestHandler(icatUrl, icatAuth, repositoryName, adminEmails);

			String[] prefixes = props.getString("metadataPrefixes").split("\\s+");
			for (String prefix : prefixes) {
				MetadataFormat format = new MetadataFormat(prefix, props.getString(prefix + ".xslt"),
						props.getString(prefix + ".namespace"), props.getString(prefix + ".schema"));
				bean.registerMetadataFormat(format);
			}
		} catch (CheckedPropertyException | FileNotFoundException | TransformerConfigurationException e) {
			logger.error(fatal, e.getMessage());
			throw new IllegalStateException(e.getMessage());
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
	public String postRequest(@Context HttpServletRequest req) {
		return bean.request(req);
	}

	@GET
	@Path("request")
	@Produces(MediaType.TEXT_XML)
	public String getRequest(@Context HttpServletRequest req) {
		return bean.request(req);
	}
}