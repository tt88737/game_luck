package com.tangluck.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_consent_logs")
public class UserConsentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(nullable = false)
    private String version;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "device_id")
    private String deviceId;

    protected UserConsentLog() {
    }

    public UserConsentLog(Long userId, String documentType, String version, Instant acceptedAt, String deviceId) {
        this.userId = userId;
        this.documentType = documentType;
        this.version = version;
        this.acceptedAt = acceptedAt;
        this.deviceId = deviceId;
    }
}
