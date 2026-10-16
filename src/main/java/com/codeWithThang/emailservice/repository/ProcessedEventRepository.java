package com.codeWithThang.emailservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.codeWithThang.emailservice.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    
}
