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
@Table(name = "wallet_accounts")
public class WalletAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "frozen_balance", nullable = false)
    private BigDecimal frozenBalance;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WalletAccount() {
    }

    public WalletAccount(Long userId, String currency, Instant now) {
        this.userId = userId;
        this.currency = currency;
        this.balance = BigDecimal.ZERO;
        this.frozenBalance = BigDecimal.ZERO;
        this.status = "active";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getFrozenBalance() {
        return frozenBalance;
    }

    public Long getId() {
        return id;
    }

    public void credit(BigDecimal amount, Instant now) {
        this.balance = this.balance.add(amount);
        this.updatedAt = now;
    }

    public void debit(BigDecimal amount, Instant now) {
        if (this.balance.subtract(this.frozenBalance).compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient available balance.");
        }
        this.balance = this.balance.subtract(amount);
        this.updatedAt = now;
    }

    public void freeze(BigDecimal amount, Instant now) {
        if (this.balance.subtract(this.frozenBalance).compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient available balance.");
        }
        this.frozenBalance = this.frozenBalance.add(amount);
        this.updatedAt = now;
    }

    public void unfreeze(BigDecimal amount, Instant now) {
        if (this.frozenBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient frozen balance.");
        }
        this.frozenBalance = this.frozenBalance.subtract(amount);
        this.updatedAt = now;
    }

    public void redeemFrozen(BigDecimal amount, Instant now) {
        if (this.frozenBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient frozen balance.");
        }
        this.frozenBalance = this.frozenBalance.subtract(amount);
        this.balance = this.balance.subtract(amount);
        this.updatedAt = now;
    }
}
