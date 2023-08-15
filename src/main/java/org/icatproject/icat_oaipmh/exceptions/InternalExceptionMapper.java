package org.icatproject.icat_oaipmh.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InternalExceptionMapper implements ExceptionMapper<InternalException> {

	@Override
	public Response toResponse(InternalException e) {
		return Response.status(e.getHttpStatusCode()).build();
	}
}
