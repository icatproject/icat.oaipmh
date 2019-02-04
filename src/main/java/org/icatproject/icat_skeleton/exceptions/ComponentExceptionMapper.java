package org.icatproject.icat_skeleton.exceptions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class ComponentExceptionMapper implements ExceptionMapper<ComponentException> {

	private final static Logger logger = LoggerFactory.getLogger(ComponentExceptionMapper.class);

	@Override
	public Response toResponse(ComponentException e) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(baos));
		logger.error("Processing: " + baos.toString());
		baos.reset();
		JsonGenerator gen = Json.createGenerator(baos);
		gen.writeStartObject().write("message", e.getClass() + " " + e.getMessage())
			.writeEnd().close();
		return Response.status(e.getHttpStatusCode()).entity(baos.toString()).build();
	}
}