package com.interview.urlshortener.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                    .toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();
    private final int codeLength;

    public ShortCodeGenerator(
            @Value("${app.short-code-length:7}") int codeLength) {
        this.codeLength = codeLength;
    }

    public String generate() {
        StringBuilder code = new StringBuilder(codeLength);

        for (int i = 0; i < codeLength; i++) {
            int position = secureRandom.nextInt(ALPHABET.length);
            code.append(ALPHABET[position]);
        }

        return code.toString();
    }
}