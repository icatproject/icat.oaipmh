package org.icatproject.icat_oaipmh.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InternalExceptionMapper implements ExceptionMapper<InternalException> {

	@Override
	public Response toResponse(InternalException e) {
		return Response.status(e.getHttpStatusCode()).build();
	}
}