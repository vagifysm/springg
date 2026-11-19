package com.example.app.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        this("%s not found with id: %s".formatted(resource, id));
    }
}
