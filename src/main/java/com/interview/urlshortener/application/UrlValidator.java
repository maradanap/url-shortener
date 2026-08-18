package com.interview.urlshortener.application;

import com.interview.urlshortener.domain.exception.InvalidUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

@Component
public class UrlValidator {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1"
    );

    public void validate(String value) {
        try {
            URI uri = new URI(value);

            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null ||
                    (!scheme.equalsIgnoreCase("http") &&
                            !scheme.equalsIgnoreCase("https"))) {
                throw new InvalidUrlException(
                        "Only HTTP and HTTPS URLs are allowed");
            }

            if (host == null || host.isBlank()) {
                throw new InvalidUrlException(
                        "URL must contain a valid host");
            }

            if (BLOCKED_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
                throw new InvalidUrlException(
                        "Local destination URLs are not allowed");
            }
        } catch (URISyntaxException exception) {
            throw new InvalidUrlException("URL syntax is invalid");
        }
    }
}