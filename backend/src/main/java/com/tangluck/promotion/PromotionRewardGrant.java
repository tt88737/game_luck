package com.tangluck.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_reward_grants")
public class PromotionRewardGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(nullable = false)
    private String status;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PromotionRewardGrant() {
    }

    public PromotionRewardGrant(Long claimId, Long userId, String currency, BigDecimal amount, Long ledgerId, String status, String rejectReason, Instant createdAt) {
        this.claimId = claimId;
        this.userId = userId;
        this.currency = currency;
        this.amount = amount;
        this.ledgerId = ledgerId;
        this.status = status;
        this.rejectReason = rejectReason;
        this.createdAt = createdAt;
    }
}
