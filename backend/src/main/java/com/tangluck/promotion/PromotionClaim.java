package com.tangluck.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "promotion_claims")
public class PromotionClaim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "period_key", nullable = false)
    private String periodKey;

    @Column(nullable = false)
    private String status;

    @Column(name = "risk_action", nullable = false)
    private String riskAction;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PromotionClaim() {
    }

    public PromotionClaim(Long userId, Long campaignId, String periodKey, String status, String riskAction, String idempotencyKey, Instant createdAt) {
        this.userId = userId;
        this.campaignId = campaignId;
        this.periodKey = periodKey;
        this.status = status;
        this.riskAction = riskAction;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }
}
