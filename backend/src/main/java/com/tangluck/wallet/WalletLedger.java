package com.tangluck.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_ledger")
public class WalletLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_account_id", nullable = false)
    private Long walletAccountId;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String direction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Column(name = "frozen_after", nullable = false)
    private BigDecimal frozenAfter;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WalletLedger() {
    }

    public WalletLedger(Long userId, Long walletAccountId, String currency, String direction, BigDecimal amount, BigDecimal balanceAfter, BigDecimal frozenAfter, String businessType, String businessId, String idempotencyKey, Instant createdAt) {
        this.userId = userId;
        this.walletAccountId = walletAccountId;
        this.currency = currency;
        this.direction = direction;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.frozenAfter = frozenAfter;
        this.businessType = businessType;
        this.businessId = businessId;
        this.idempotencyKey = idempotencyKey;
        this.status = "posted";
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDirection() {
        return direction;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public BigDecimal getFrozenAfter() {
        return frozenAfter;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
