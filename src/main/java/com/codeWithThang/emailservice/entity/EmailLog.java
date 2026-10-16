package com.codeWithThang.emailservice.entity;

import java.time.LocalDateTime;

import com.codeWithThang.emailservice.common.BaseEntity;
import com.codeWithThang.emailservice.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "email_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class EmailLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String recipient;

    @Column(nullable = false, length = 500)
    String subject;

    @Column(columnDefinition = "TEXT")
    String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    EmailStatus status;

    @Column(name = "event_type")
    String eventType;

    // idempotency key — tránh gửi mail trùng cùng một event
    @Column(name = "event_id", unique = true)
    String eventId;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    String errorMsg;

    @Column(nullable = false)
    Integer attempts = 0;

    @Column(name = "next_retry_at")
    LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    LocalDateTime sentAt;
}
