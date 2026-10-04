package com.jansunwai.exception;

import org.springframework.http.HttpStatus;

/** Thrown anywhere in the app when a request should fail with a clear HTTP error. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
