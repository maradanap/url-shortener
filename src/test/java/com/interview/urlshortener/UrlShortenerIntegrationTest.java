package com.interview.urlshortener;

import com.interview.urlshortener.infrastructure.persistence.ClickEventRepository;
import com.interview.urlshortener.infrastructure.persistence.ShortUrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlShortenerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        clearDatabaseAndCache();
    }

    @AfterEach
    void tearDown() {
        clearDatabaseAndCache();
    }

    @Test
    void shouldCreateUrlWithCustomAlias() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/products/123",
                                  "customAlias": "integration-01",
                                  "expiresAt": "2026-12-31T23:59:59Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "http://localhost:8080/integration-01"))
                .andExpect(jsonPath("$.shortCode")
                        .value("integration-01"))
                .andExpect(jsonPath("$.shortUrl")
                        .value("http://localhost:8080/integration-01"))
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://example.com/products/123"))
                .andExpect(jsonPath("$.active")
                        .value(true));
    }

    @Test
    void shouldCreateGeneratedCode() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://spring.io"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode")
                        .value(matchesPattern(
                                "^[A-Za-z0-9]{7}$")))
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://spring.io"))
                .andExpect(jsonPath("$.active")
                        .value(true));
    }

    @Test
    void shouldCreateThenRetrieveMetadata() throws Exception {
        createUrl("metadata-01",
                "https://example.com/metadata");

        mockMvc.perform(get(
                        "/api/v1/urls/metadata-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode")
                        .value("metadata-01"))
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://example.com/metadata"))
                .andExpect(jsonPath("$.active")
                        .value(true));
    }

    @Test
    void shouldRedirectAndIncrementAnalytics() throws Exception {
        createUrl("redirect-01",
                "https://example.com/destination");

        mockMvc.perform(get("/redirect-01"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "https://example.com/destination"));

        mockMvc.perform(get(
                        "/api/v1/urls/redirect-01/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode")
                        .value("redirect-01"))
                .andExpect(jsonPath("$.totalClicks")
                        .value(1))
                .andExpect(jsonPath("$.lastAccessedAt")
                        .isNotEmpty());
    }

    @Test
    void multipleRedirectsShouldIncreaseClickCount()
            throws Exception {

        createUrl("clicks-01",
                "https://example.com/clicks");

        mockMvc.perform(get("/clicks-01"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/clicks-01"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/clicks-01"))
                .andExpect(status().isFound());

        mockMvc.perform(get(
                        "/api/v1/urls/clicks-01/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks")
                        .value(3));
    }

    @Test
    void duplicateAliasShouldReturnConflict()
            throws Exception {

        createUrl("duplicate-01",
                "https://example.com/first");

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com/second",
                                  "customAlias": "duplicate-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.error")
                        .value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value(matchesPattern(
                                ".*duplicate-01.*")));
    }

    @Test
    void invalidSchemeShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "javascript:alert(1)",
                                  "customAlias": "invalid-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400));
    }

    @Test
    void missingUrlShouldReturnFieldError()
            throws Exception {

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customAlias": "missing-url"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath(
                        "$.fieldErrors.url")
                        .exists());
    }

    @Test
    void invalidAliasShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com",
                                  "customAlias": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath(
                        "$.fieldErrors.customAlias")
                        .exists());
    }

    @Test
    void pastExpirationShouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "url": "https://example.com",
                                  "customAlias": "expired-int",
                                  "expiresAt": "2025-01-01T00:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.message")
                        .value(matchesPattern(
                                ".*future.*")));
    }

    @Test
    void unknownRedirectShouldReturnNotFound()
            throws Exception {

        mockMvc.perform(get("/not-found-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404));
    }

    @Test
    void unknownMetadataShouldReturnNotFound()
            throws Exception {

        mockMvc.perform(get(
                        "/api/v1/urls/not-found-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(404));
    }

    @Test
    void shouldDisableUrlAndRejectFutureRedirects()
            throws Exception {

        createUrl("disable-01",
                "https://example.com/disable");

        // Populate Caffeine before disabling the URL.
        mockMvc.perform(get("/disable-01"))
                .andExpect(status().isFound());

        mockMvc.perform(delete(
                        "/api/v1/urls/disable-01"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/disable-01"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status")
                        .value(410));

        mockMvc.perform(get(
                        "/api/v1/urls/disable-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active")
                        .value(false));
    }

    private void createUrl(
            String alias,
            String destination) throws Exception {

        String request = """
                {
                  "url": "%s",
                  "customAlias": "%s"
                }
                """.formatted(destination, alias);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());
    }

    private void clearDatabaseAndCache() {
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();

        Cache cache = cacheManager.getCache("shortUrls");

        if (cache != null) {
            cache.clear();
        }
    }
}