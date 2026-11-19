package com.example.app.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a request clashes with existing data (e.g. duplicate unique key). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
