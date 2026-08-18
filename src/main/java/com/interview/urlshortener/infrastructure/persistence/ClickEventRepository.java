package com.interview.urlshortener.infrastructure.persistence;

import com.interview.urlshortener.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortUrlShortCode(String shortCode);

    @Query("""
            select max(c.clickedAt)
            from ClickEvent c
            where c.shortUrl.shortCode = :shortCode
            """)
    Optional<Instant> findLastAccessedAt(@Param("shortCode") String shortCode);
}