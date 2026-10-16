package com.codeWithThang.emailservice.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeWithThang.emailservice.entity.EmailLog;
import com.codeWithThang.emailservice.repository.EmailLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults (level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class EmailRetryService {

    EmailLogRepository emailLogRepository;
    EmailLogService emailLogService;
    EmailService emailService;

    @Scheduled (fixedDelay = 3000)
    public void retryFailedEmails() {
        recoverStuckEmails();

        List<Long> ids = emailLogRepository.findDueRetryIds(100);

        for (Long id : ids) {
            try {
                retryOne(id);
            } catch (RuntimeException e) {
                log.error("Retry failed for email log id {}", id, e);
            }
        }
    }

    private void retryOne(Long id) {
        boolean claimed = emailLogService.claimRetry(id);

        if (!claimed) {
            return;
        }

        EmailLog emailLog = emailLogService.getById(id);

        log.info(
                "Claimed email log id={} eventId={} recipient={}",
                id,
                emailLog.getEventId(),
                emailLog.getRecipient()
        );

        emailService.retryEmail(emailLog);
    }

    private void recoverStuckEmails() {
        List <Long> ids = emailLogRepository.findStuckProcessingIds(100);

        for (Long id : ids) {
            try {
                boolean recovered = emailLogService.recoverStuckProcessing(id);

                if (recovered) {
                    log.warn("Recovered stuck email log id = {}", id);
                }
            } catch (RuntimeException e) {
                log.error(
                    "Failed to recover stuck email log id={}", id, e
                );
            }
        }
    }
}
