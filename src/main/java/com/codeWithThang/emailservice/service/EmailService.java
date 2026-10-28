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

        String subject = "Xác nhận email đăng ký tài khoản";
        String body = buildVerificationEmailBody(event.firstName(), event.verificationToken());

        log.info("Preparing verification email for {} <{}>", event.firstName(), event.email());

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
            log.info("Verification email sent successfully to {}", event.email());
        } catch (MessagingException | MailException e) {
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
            log.error("Failed to send verification email to {}: {}", event.email(), e.getMessage());
        }
    }

    private String buildVerificationEmailBody(String firstName, String token) {
        String verificationUrl = "http://localhost:5173/verify-email?token=" + token;
        return """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #4F46E5;">Xin chào %s!</h2>
                        <p>Cảm ơn bạn đã đăng ký tài khoản tại cửa hàng của chúng tôi.</p>
                        <p>Vui lòng xác nhận email của bạn bằng cách nhấp vào nút bên dưới:</p>
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" 
                               style="background: linear-gradient(to right, #4F46E5, #7C3AED); 
                                      color: white; 
                                      padding: 12px 30px; 
                                      text-decoration: none; 
                                      border-radius: 8px; 
                                      font-weight: bold;
                                      display: inline-block;">
                                Xác nhận Email
                            </a>
                        </div>
                        <p>Hoặc sử dụng liên kết sau:</p>
                        <p style="word-break: break-all; color: #4F46E5;">%s</p>
                        <p style="color: #666; font-size: 14px;">Liên kết này sẽ hết hạn sau 15 phút.</p>
                        <br/>
                        <p>Trân trọng,</p>
                        <p><strong>Đội ngũ hỗ trợ</strong></p>
                    </div>
                </body>
                </html>
                """.formatted(firstName, verificationUrl, verificationUrl);
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
