package com.codeWithThang.emailservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    /**
     * Tự động repair checksum rồi mới migrate.
     * Cần thiết khi nội dung file migration đã chạy bị thay đổi (dev env).
     * Trên production nên bỏ bean này và không bao giờ sửa file migration đã apply.
     */
    @Bean
    public FlywayMigrationStrategy flywayRepairStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
