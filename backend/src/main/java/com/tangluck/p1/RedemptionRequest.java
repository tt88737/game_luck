package com.tangluck.p1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "redemption_requests")
public class RedemptionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "redemption_id", nullable = false)
    private String redemptionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sc_amount", nullable = false)
    private BigDecimal scAmount;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private String status;

    @Column(name = "sandbox_only", nullable = false)
    private boolean sandboxOnly;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "freeze_ledger_id")
    private Long freezeLedgerId;

    @Column(name = "review_reason")
    private String reviewReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RedemptionRequest() {
    }

    public RedemptionRequest(String redemptionId, Long userId, BigDecimal scAmount, String method, String idempotencyKey, Instant now) {
        this.redemptionId = redemptionId;
        this.userId = userId;
        this.scAmount = scAmount;
        this.method = method;
        this.status = "reviewing";
        this.sandboxOnly = true;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getRedemptionId() {
        return redemptionId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getScAmount() {
        return scAmount;
    }

    public String getMethod() {
        return method;
    }

    public String getStatus() {
        return status;
    }

    public boolean isSandboxOnly() {
        return sandboxOnly;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void attachFreezeLedger(Long ledgerId, Instant now) {
        this.freezeLedgerId = ledgerId;
        this.updatedAt = now;
    }
}
