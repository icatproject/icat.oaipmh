package org.icatproject.icat_skeleton.exceptions;

@SuppressWarnings("serial")
public class ComponentException extends Exception {

	private String message;
	private int httpStatusCode;

	public ComponentException(String message, int httpStatusCode) {
		this.message = message;
		this.httpStatusCode = httpStatusCode;
	}

	public String getMessage() {
		return message;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}

}