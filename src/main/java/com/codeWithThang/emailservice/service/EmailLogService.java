package com.codeWithThang.emailservice.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeWithThang.emailservice.entity.EmailLog;
import com.codeWithThang.emailservice.enums.EmailStatus;
import com.codeWithThang.emailservice.repository.EmailLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service 
@RequiredArgsConstructor 
@FieldDefaults (level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class EmailLogService {
    EmailLogRepository emailLogRepository;

    @Transactional 
    public EmailLog createPending(
        String eventId,
        String eventType,
        String recipient,
        String subject,
        String body
    ) {
        EmailLog emailLog = EmailLog.builder()
            .eventId(eventId)
            .eventType(eventType)
            .recipient(recipient)
            .subject(subject)
            .body(body)
            .status(EmailStatus.PENDING)
            .attempts(0)
            .build();
        
        return emailLogRepository.save(emailLog);
    }

    @Transactional 
    public void markSent(String eventId) {
        EmailLog emailLog = emailLogRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException(
                    "Email log not found: " + eventId
                ));
        emailLog.setStatus(EmailStatus.SENT);
        emailLog.setSentAt(LocalDateTime.now());
        emailLog.setNextRetryAt(null);
        emailLog.setErrorMsg(null);
    }

    @Transactional 
    public void markFailed(
        String eventId,
        String errorMessage,
        LocalDateTime nextRetryAt) {

        EmailLog emailLog = emailLogRepository.findByEventId(eventId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Email log not found: " + eventId
                        )
                );

        int attempts = emailLog.getAttempts() + 1;
        emailLog.setAttempts(attempts);
        emailLog.setErrorMsg(errorMessage);
        emailLog.setSentAt(null);

        if (attempts >= 5) {
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setNextRetryAt(null);

            // Đã hết số lần thử, không retry tiếp.
            return;
        }

        emailLog.setStatus(EmailStatus.FAILED);
        emailLog.setNextRetryAt(nextRetryAt);
    }

    @Transactional 
    public boolean claimEvent (
        String eventId,
        String eventType,
        String recipient,
        String subject,
        String body
    ) {
        int inserted = emailLogRepository.claimEvent(eventId, eventType, recipient, subject, body);

        return inserted == 1;
    }

    @Transactional 
    public boolean claimRetry(Long id) {
        return  emailLogRepository.claimRetry(id) == 1;
    }

    @Transactional (readOnly = true)
    public EmailLog getById(Long id) {
        return emailLogRepository.findEmailLogById(id)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Email log not found: " + id
                        )
                );
    }

    @Transactional 
    public boolean recoverStuckProcessing(Long id) {
        return emailLogRepository.recoverStuckProcessing(id) == 1;
    }
}
