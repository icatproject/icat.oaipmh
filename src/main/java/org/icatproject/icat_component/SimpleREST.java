package org.icatproject.icat_component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.icatproject.authentication.AuthnException;
import org.icatproject.authentication.PasswordChecker;
import org.icatproject.utils.AddressChecker;
import org.icatproject.utils.AddressCheckerException;
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

	private Map<String, String> passwordtable;
	private AddressChecker addressChecker;
	private String mechanism;

	@PostConstruct
	private void init() {
		CheckedProperties props = new CheckedProperties();
		try {
			props.loadFromResource("run.properties");

			// Build the passwordtable out of user.list and
			// user.<usern>.password
			passwordtable = new HashMap<String, String>();
			String[] users = props.getString("user.list").split("\\s+");

			String msg = "users configured [" + users.length + "]: ";
			for (String user : users) {
				passwordtable.put(user, props.getString("user." + user + ".password"));
				msg = msg + user + " ";
			}
			logger.debug(msg);

			if (props.has("ip")) {
				String authips = props.getString("ip");
				try {
					addressChecker = new AddressChecker(authips);
				} catch (Exception e) {
					msg = "Problem creating AddressChecker with information from run.properties " + e.getMessage();
					logger.error(fatal, msg);
					throw new IllegalStateException(msg);
				}
			}

			// Note that the mechanism is optional
			if (props.has("mechanism")) {
				mechanism = props.getString("mechanism");
			}

		} catch (CheckedPropertyException e) {
			logger.error(fatal, e.getMessage());
			throw new IllegalStateException(e.getMessage());
		}

		logger.debug("Initialised SIMPLE_Authenticator");
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
	@Path("authenticate")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public String authenticate(@FormParam("json") String jsonString) throws AuthnException {

		ByteArrayInputStream s = new ByteArrayInputStream(jsonString.getBytes());

		String username = null;
		String password = null;
		String ip = null;
		try (JsonReader r = Json.createReader(s)) {
			JsonObject o = r.readObject();
			for (JsonValue c : o.getJsonArray("credentials")) {
				JsonObject credential = (JsonObject) c;
				if (credential.containsKey("username")) {
					username = credential.getString("username");
				} else if (credential.containsKey("password")) {
					password = credential.getString("password");
				}
			}
			if (o.containsKey("ip")) {
				ip = o.getString("ip");
			}

		}

		logger.debug("Login request by: " + username);

		if (username == null || username.isEmpty()) {
			throw new AuthnException(HttpURLConnection.HTTP_FORBIDDEN, "username cannot be null or empty.");
		}

		if (password == null || password.isEmpty()) {
			throw new AuthnException(HttpURLConnection.HTTP_FORBIDDEN, "password cannot be null or empty.");
		}

		if (addressChecker != null) {
			try {
				if (!addressChecker.check(ip)) {
					throw new AuthnException(HttpURLConnection.HTTP_FORBIDDEN,
							"icat.component does not allow log in from your IP address " + ip);
				}
			} catch (AddressCheckerException e) {
				throw new AuthnException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getClass() + " " + e.getMessage());
			}
		}

		String encodedPassword = passwordtable.get(username);
		if (!PasswordChecker.verify(password, encodedPassword)) {
			throw new AuthnException(HttpURLConnection.HTTP_FORBIDDEN, "The username and password do not match ");
		}

		logger.info(username + " logged in succesfully" + (mechanism != null ? " by " + mechanism : ""));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator gen = Json.createGenerator(baos)) {
			gen.writeStartObject().write("username", username);
			if (mechanism != null) {
				gen.write("mechanism", mechanism);
			}
			gen.writeEnd();
		}
		return baos.toString();

	}

	@GET
	@Path("description")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public String getDescription() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (JsonGenerator gen = Json.createGenerator(baos)) {
			gen.writeStartObject().writeStartArray("keys");
			gen.writeStartObject().write("name", "username").writeEnd();
			gen.writeStartObject().write("name", "password").write("hide", true).writeEnd();
			gen.writeEnd().writeEnd();
		}
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
