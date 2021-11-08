package org.icatproject.icat_oaipmh.exceptions;

@SuppressWarnings("serial")
public class InternalException extends Exception {

	private int httpStatusCode;

	public InternalException(String message, Throwable cause) {
		super(message, cause);
		this.httpStatusCode = 500;
	}

	public InternalException() {
		this.httpStatusCode = 500;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}
}