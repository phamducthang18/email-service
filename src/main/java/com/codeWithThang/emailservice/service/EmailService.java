package com.codeWithThang.emailservice.service;

import com.codeWithThang.emailservice.entity.EmailLog;
import com.codeWithThang.emailservice.event.UserRegisteredEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {

    EmailLogService emailLogService;
    JavaMailSender mailSender;

    public void handleUserRegistered(UserRegisteredEvent event) {

        String subject = "Chào mừng bạn tới với dịch vụ!";
        String body = buildWelcomeEmailBody(event.firstName());

        log.info("Preparing welcome email for {} <{}>", event.firstName(), event.email());

        boolean claimed = emailLogService.claimEvent(event.eventId(), event.eventType(), event.email(), subject, body);
        if (!claimed) {
            log.info("Event {} has already been claimed. skipping email.", event.eventId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(event.email());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);

            emailLogService.markSent(event.eventId());
            log.info("Welcome email sent successfully to {}", event.email());
        } catch (MessagingException e) {
            try {
                emailLogService.markFailed(
                    event.eventId(),
                    e.getMessage(),
                    LocalDateTime.now().plusMinutes(1)
                );
            } catch (Exception ex) {
                e.addSuppressed(ex);
                log.error("Failed to persist email failure for event {} ", event.eventId(), ex);
            }
            log.error("Failed to send welcome email to {}: {}", event.email(), e.getMessage());
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    private String buildWelcomeEmailBody(String firstName) {
        return """
                <html>
                <body>
                    <h2>Xin chào %s!</h2>
                    <p>Chào mừng bạn đã đăng ký tài khoản thành công.</p>
                    <p>Cảm ơn bạn đã tin tưởng và sử dụng dịch vụ của chúng tôi.</p>
                    <br/>
                    <p>Trân trọng,</p>
                    <p><strong>Đội ngũ hỗ trợ</strong></p>
                </body>
                </html>
                """.formatted(firstName);
    }

    public void retryEmail(EmailLog emailLog) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailLog.getRecipient());
            helper.setSubject(emailLog.getSubject());
            helper.setText(emailLog.getBody(), true);

            mailSender.send(message);

            emailLogService.markSent(emailLog.getEventId());

            log.info(
                    "Retry email sent successfully. eventId={}, recipient={}",
                    emailLog.getEventId(),
                    emailLog.getRecipient()
            );

        } catch (MessagingException | MailException e) {
            String errorMessage = e.getMessage() != null
                    ? e.getMessage()
                    : e.getClass().getSimpleName();

            try {
                emailLogService.markFailed(
                        emailLog.getEventId(),
                        errorMessage,
                        LocalDateTime.now().plusMinutes(1)
                );
            } catch (RuntimeException loggingException) {
                e.addSuppressed(loggingException);
                log.error(
                        "Failed to update retry status for event {}",
                        emailLog.getEventId(),
                        loggingException
                );
            }

            log.error(
                    "Retry email failed. eventId={}",
                    emailLog.getEventId(),
                    e
            );
        }
    }

}
