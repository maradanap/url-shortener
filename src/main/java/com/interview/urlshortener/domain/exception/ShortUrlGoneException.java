package com.interview.urlshortener.domain.exception;

public class ShortUrlGoneException extends RuntimeException {

    public ShortUrlGoneException(String shortCode) {
        super("Short URL is inactive or expired: " + shortCode);
    }
}