package com.tangluck.p1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

import static com.tangluck.p1.P1Dtos.KycApplicationRequest;

@Entity
@Table(name = "kyc_applications")
public class KycApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "address_line", nullable = false)
    private String addressLine;

    @Column(name = "state_code", nullable = false)
    private String stateCode;

    @Column(nullable = false)
    private String status;

    @Column(name = "review_reason")
    private String reviewReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KycApplication() {
    }

    public KycApplication(Long userId, KycApplicationRequest request, Instant now) {
        this.userId = userId;
        this.legalName = request.legalName();
        this.birthDate = request.birthDate();
        this.addressLine = request.addressLine();
        this.stateCode = request.stateCode();
        this.status = "reviewing";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getUserId() {
        return userId;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getStatus() {
        return status;
    }

    public String getReviewReason() {
        return reviewReason;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void replace(KycApplicationRequest request, Instant now) {
        this.legalName = request.legalName();
        this.birthDate = request.birthDate();
        this.addressLine = request.addressLine();
        this.stateCode = request.stateCode();
        this.status = "reviewing";
        this.reviewReason = null;
        this.updatedAt = now;
    }

    public void approve(Instant now) {
        this.status = "approved";
        this.reviewReason = "approved by ops";
        this.updatedAt = now;
    }
}
