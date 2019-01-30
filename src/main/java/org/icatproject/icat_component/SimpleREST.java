package org.icatproject.icat_component;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.icatproject.icat_component.exceptions.ComponentException;
import org.icatproject.utils.CheckedProperties;
import org.icatproject.utils.CheckedProperties.CheckedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/* Mapped name is to avoid name clashes */
@Path("/")
@Stateless
public class SimpleREST {

	private static final Logger logger = LoggerFactory.getLogger(SimpleREST.class);
	private static final Marker fatal = MarkerFactory.getMarker("FATAL");

	private String required_property;
	private String message;

	@PostConstruct
	private void init() {
		CheckedProperties props = new CheckedProperties();
		try {
			props.loadFromResource("run.properties");

			required_property = props.getString("required_property");

			if (props.has("message")) {
				message = props.getString("message");
			} else {
				message = null;
			}

		} catch (CheckedPropertyException e) {
			logger.error(fatal, e.getMessage());
			throw new IllegalStateException(e.getMessage());
		}

		logger.info("Initialised SimpleREST with required_property: " + required_property);
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

	@GET
	@Path("message")
	@Produces(MediaType.APPLICATION_JSON)
	public String message() throws ComponentException {
		if (message == null) {
			throw new ComponentException("example for not found exception mapping", HttpURLConnection.HTTP_NOT_FOUND);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = Json.createGenerator(baos);
		gen.writeStartObject().write("message", message).writeEnd();
		gen.close();
		return baos.toString();
	}

	@GET
	@Path("hello")
	@Produces(MediaType.APPLICATION_JSON)
	public String hello() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = Json.createGenerator(baos);
		gen.writeStartObject().write("Hello", "World").writeEnd();
		gen.close();
		return baos.toString();
	}

}
