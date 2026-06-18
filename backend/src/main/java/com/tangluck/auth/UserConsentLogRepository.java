package com.tangluck.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserConsentLogRepository extends JpaRepository<UserConsentLog, Long> {
    List<UserConsentLog> findByUserId(Long userId);
}
