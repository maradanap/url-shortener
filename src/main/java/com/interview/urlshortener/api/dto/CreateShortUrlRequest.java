package com.interview.urlshortener.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateShortUrlRequest(

        @NotBlank(message = "URL is required")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        String url,

        @Pattern(
                regexp = "^[A-Za-z0-9_-]{4,32}$",
                message = "Custom alias must contain 4 to 32 letters, numbers, underscores or hyphens"
        )
        String customAlias,

        Instant expiresAt
) {
}