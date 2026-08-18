package com.interview.urlshortener.application;

import com.interview.urlshortener.domain.exception.InvalidUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlValidatorTest {

    private UrlValidator urlValidator;

    @BeforeEach
    void setUp() {
        urlValidator = new UrlValidator();
    }

    @Test
    void shouldAcceptValidHttpsUrl() {
        assertThatNoException()
                .isThrownBy(() ->
                        urlValidator.validate(
                                "https://example.com/products/123"));
    }

    @Test
    void shouldAcceptValidHttpUrl() {
        assertThatNoException()
                .isThrownBy(() ->
                        urlValidator.validate(
                                "http://example.com"));
    }

    @Test
    void shouldRejectUnsupportedScheme() {
        assertThatThrownBy(() ->
                urlValidator.validate(
                        "ftp://example.com/file.txt"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("HTTP and HTTPS");
    }

    @Test
    void shouldRejectUrlWithoutScheme() {
        assertThatThrownBy(() ->
                urlValidator.validate("example.com"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("HTTP and HTTPS");
    }

    @Test
    void shouldRejectUrlWithoutHost() {
        assertThatThrownBy(() ->
                urlValidator.validate("https:///products/123"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("valid host");
    }

    @Test
    void shouldRejectLocalhost() {
        assertThatThrownBy(() ->
                urlValidator.validate(
                        "http://localhost/internal"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("Local destination");
    }

    @Test
    void shouldRejectLoopbackAddress() {
        assertThatThrownBy(() ->
                urlValidator.validate(
                        "http://127.0.0.1/admin"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("Local destination");
    }

    @Test
    void shouldRejectInvalidSyntax() {
        assertThatThrownBy(() ->
                urlValidator.validate(
                        "https://exa mple.com"))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessageContaining("syntax");
    }
}