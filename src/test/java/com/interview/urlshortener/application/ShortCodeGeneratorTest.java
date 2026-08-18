package com.interview.urlshortener.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTest {

    @Test
    void shouldGenerateConfiguredLength() {
        ShortCodeGenerator generator =
                new ShortCodeGenerator(7);

        String result = generator.generate();

        assertThat(result).hasSize(7);
    }

    @Test
    void shouldUseOnlyBase62Characters() {
        ShortCodeGenerator generator =
                new ShortCodeGenerator(7);

        for (int attempt = 0; attempt < 100; attempt++) {
            String result = generator.generate();

            assertThat(result)
                    .matches("^[A-Za-z0-9]{7}$");
        }
    }

    @Test
    void shouldSupportAnotherConfiguredLength() {
        ShortCodeGenerator generator =
                new ShortCodeGenerator(10);

        String result = generator.generate();

        assertThat(result)
                .hasSize(10)
                .matches("^[A-Za-z0-9]{10}$");
    }
}