package com.codeWithThang.emailservice.consumer;

import com.codeWithThang.emailservice.event.UserRegisteredEvent;
import com.codeWithThang.emailservice.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class EmailEventConsumer {

    EmailService emailService;
    ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${spring.kafka.consumer.topic:user-registered}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        log.info("Received email event: {}", message);
        try {
            UserRegisteredEvent event = objectMapper.readValue(message, UserRegisteredEvent.class);
            emailService.handleUserRegistered(event);
        } catch (Exception e) {
            log.error("Failed to process email event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process email event", e);
        }
    }
}
