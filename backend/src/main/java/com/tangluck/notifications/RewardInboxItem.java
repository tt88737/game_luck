package com.tangluck.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reward_inbox")
public class RewardInboxItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(name = "reward_currency", nullable = false)
    private String rewardCurrency;

    @Column(name = "reward_amount", nullable = false)
    private BigDecimal rewardAmount;

    @Column(nullable = false)
    private String status;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private String sourceId;

    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    protected RewardInboxItem() {
    }

    public RewardInboxItem(Long userId, NotificationDtos.ManualGrantRequest request, Instant now) {
        this.userId = userId;
        this.title = request.title();
        this.message = request.message();
        this.rewardCurrency = request.rewardCurrency();
        this.rewardAmount = request.rewardAmount();
        this.status = "claimable";
        this.sourceType = request.sourceType();
        this.sourceId = request.sourceId();
        this.createdAt = now;
        this.expiresAt = now.plusSeconds(30L * 24 * 60 * 60);
    }

    public void claim(Long ledgerId, Instant now) {
        this.status = "claimed";
        this.ledgerId = ledgerId;
        this.claimedAt = now;
    }

    public void expire() {
        this.status = "expired";
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getRewardCurrency() { return rewardCurrency; }
    public BigDecimal getRewardAmount() { return rewardAmount; }
    public String getStatus() { return status; }
    public String getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public Long getLedgerId() { return ledgerId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getClaimedAt() { return claimedAt; }
}
