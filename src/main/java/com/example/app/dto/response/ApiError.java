package com.example.app.dto.response;

/** A single validation problem: which field, and why. */
public record ApiError(String field, String message) {
}
