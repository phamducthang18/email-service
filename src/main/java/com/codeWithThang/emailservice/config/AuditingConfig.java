package com.codeWithThang.emailservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

    /**
     * Cung cấp thông tin "ai đang thực hiện thao tác" cho @CreatedBy / @LastModifiedBy.
     * Email service nhận event từ Kafka (không có user context) nên dùng "system".
     * Nếu sau này có thêm REST API với auth, có thể lấy từ SecurityContextHolder tại đây.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }
}
