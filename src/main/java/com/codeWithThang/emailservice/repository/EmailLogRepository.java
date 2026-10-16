package com.codeWithThang.emailservice.repository;

import com.codeWithThang.emailservice.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Optional<EmailLog> findByEventId(String eventId);
    boolean existsByEventId(String eventId);

    @Modifying
    @Query(value = """
        INSERT INTO email_logs (
            recipient,
            subject,
            body,
            status,
            event_type,
            event_id,
            attempts,
            created_at,
            updated_at
        )
        VALUES (
            :recipient,
            :subject,
            :body,
            'PROCESSING',
            :eventType,
            :eventId,
            0,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT (event_id) DO NOTHING
        """, nativeQuery = true)
    int claimEvent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("recipient") String recipient,
            @Param("subject") String subject,
            @Param("body") String body
    );

    @Query(value = """
        SELECT id
        FROM email_logs
        WHERE status = 'FAILED'
        AND next_retry_at IS NOT NULL
        AND next_retry_at <= CURRENT_TIMESTAMP
        ORDER BY next_retry_at ASC
        LIMIT :batchSize
        """, nativeQuery = true)
    List<Long> findDueRetryIds(@Param("batchSize") int batchSize);

    @Modifying
    @Query(value = """
        UPDATE email_logs
        SET status = 'PROCESSING',
            updated_at = CURRENT_TIMESTAMP
        WHERE id = :id
        AND status = 'FAILED'
        AND next_retry_at IS NOT NULL
        AND next_retry_at <= CURRENT_TIMESTAMP
        """, nativeQuery = true)
    int claimRetry(@Param("id") Long id);

    @Query ("""
            SELECT e
            FROM EmailLog e
            WHERE e.id = :id
            """)
    Optional<EmailLog> findEmailLogById(@Param ("id") Long id);

    @Query(value = """
        SELECT id
        FROM email_logs
        WHERE status = 'PROCESSING'
        AND updated_at < CURRENT_TIMESTAMP - INTERVAL '5 minutes'
        ORDER BY updated_at ASC
        LIMIT :batchSize
        """, nativeQuery = true)
    List<Long> findStuckProcessingIds(
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query(value = """
        UPDATE email_logs
        SET status = 'FAILED',
            error_msg = 'Recovered stale PROCESSING record',
            next_retry_at = CURRENT_TIMESTAMP,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = :id
        AND status = 'PROCESSING'
        AND updated_at < CURRENT_TIMESTAMP - INTERVAL '5 minutes'
        AND attempts < 5
        """, nativeQuery = true)
    int recoverStuckProcessing(@Param("id") Long id);
}
