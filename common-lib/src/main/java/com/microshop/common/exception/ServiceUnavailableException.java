package com.microshop.common.exception;

import org.springframework.http.HttpStatus;

/** A downstream dependency (another service) could not be reached. */
public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
