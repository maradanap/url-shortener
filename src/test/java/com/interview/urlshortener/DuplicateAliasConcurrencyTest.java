package com.interview.urlshortener;

import com.interview.urlshortener.api.dto.CreateShortUrlRequest;
import com.interview.urlshortener.application.UrlShorteningService;
import com.interview.urlshortener.domain.exception.ShortUrlConflictException;
import com.interview.urlshortener.infrastructure.persistence.ClickEventRepository;
import com.interview.urlshortener.infrastructure.persistence.ShortUrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DuplicateAliasConcurrencyTest {

    private static final int THREAD_COUNT = 8;

    @Autowired
    private UrlShorteningService urlShorteningService;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();

        executorService =
                Executors.newFixedThreadPool(
                        THREAD_COUNT);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(
                5, TimeUnit.SECONDS);

        clickEventRepository.deleteAll();
        shortUrlRepository.deleteAll();
    }

    @Test
    void exactlyOneConcurrentAliasCreationShouldSucceed()
            throws Exception {

        String sharedAlias = "concurrent-01";

        CreateShortUrlRequest request =
                new CreateShortUrlRequest(
                        "https://example.com/concurrent",
                        sharedAlias,
                        null);

        CountDownLatch ready =
                new CountDownLatch(THREAD_COUNT);
        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<Outcome>> futures =
                new ArrayList<>();

        for (int index = 0;
             index < THREAD_COUNT;
             index++) {

            Callable<Outcome> task = () -> {
                ready.countDown();

                if (!start.await(
                        5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Start signal timed out");
                }

                try {
                    urlShorteningService.create(request);
                    return Outcome.SUCCESS;
                } catch (ShortUrlConflictException exception) {
                    return Outcome.CONFLICT;
                }
            };

            futures.add(executorService.submit(task));
        }

        assertThat(ready.await(
                5, TimeUnit.SECONDS)).isTrue();

        start.countDown();

        List<Outcome> outcomes =
                collectResults(futures);

        assertThat(outcomes)
                .filteredOn(
                        outcome ->
                                outcome == Outcome.SUCCESS)
                .hasSize(1);

        assertThat(outcomes)
                .filteredOn(
                        outcome ->
                                outcome == Outcome.CONFLICT)
                .hasSize(THREAD_COUNT - 1);

        assertThat(shortUrlRepository.count())
                .isEqualTo(1);

        assertThat(shortUrlRepository
                .findByShortCode(sharedAlias))
                .isPresent();
    }

    private List<Outcome> collectResults(
            List<Future<Outcome>> futures)
            throws InterruptedException,
            ExecutionException,
            java.util.concurrent.TimeoutException {

        List<Outcome> outcomes =
                new ArrayList<>();

        for (Future<Outcome> future : futures) {
            outcomes.add(
                    future.get(
                            10,
                            TimeUnit.SECONDS));
        }

        return outcomes;
    }

    private enum Outcome {
        SUCCESS,
        CONFLICT
    }
}