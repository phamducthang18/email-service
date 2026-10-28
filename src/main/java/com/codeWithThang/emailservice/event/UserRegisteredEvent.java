
package com.codeWithThang.emailservice.event;

public record UserRegisteredEvent(
        String eventId,
        Long userId,
        String email,
        String firstName,
        String eventType,
        String verificationToken
) {
}
