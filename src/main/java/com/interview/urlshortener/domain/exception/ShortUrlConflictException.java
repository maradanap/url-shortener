package com.interview.urlshortener.domain.exception;

public class ShortUrlConflictException extends RuntimeException {

    public ShortUrlConflictException(String shortCode) {
        super("Short code is already in use: " + shortCode);
    }
}