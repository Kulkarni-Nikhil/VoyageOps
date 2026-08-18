package com.voyageops.app.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a required external dependency is unavailable (missing API key,
 * upstream outage, or timeout) and no sensible fallback exists.
 */
public class ServiceUnavailableException extends ApiException {

    public ServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}