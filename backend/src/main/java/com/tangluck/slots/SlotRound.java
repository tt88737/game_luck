package com.tangluck.slots;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "slot_rounds")
public class SlotRound {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_id", nullable = false)
    private String roundId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "game_code", nullable = false)
    private String gameCode;

    @Column(nullable = false)
    private String currency;

    @Column(name = "bet_amount", nullable = false)
    private BigDecimal betAmount;

    @Column(name = "payout_amount", nullable = false)
    private BigDecimal payoutAmount;

    @Column(nullable = false)
    private BigDecimal multiplier;

    @Column(name = "reel_result_json", nullable = false)
    private String reelResultJson;

    @Column(nullable = false)
    private String status;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "debit_ledger_id")
    private Long debitLedgerId;

    @Column(name = "credit_ledger_id")
    private Long creditLedgerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SlotRound() {
    }

    public SlotRound(String roundId, Long userId, String gameCode, String currency, BigDecimal betAmount, BigDecimal payoutAmount, BigDecimal multiplier, String reelResultJson, String status, String idempotencyKey, Instant createdAt) {
        this.roundId = roundId;
        this.userId = userId;
        this.gameCode = gameCode;
        this.currency = currency;
        this.betAmount = betAmount;
        this.payoutAmount = payoutAmount;
        this.multiplier = multiplier;
        this.reelResultJson = reelResultJson;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public void attachLedgers(Long debitLedgerId, Long creditLedgerId) {
        this.debitLedgerId = debitLedgerId;
        this.creditLedgerId = creditLedgerId;
    }

    public String getRoundId() { return roundId; }
    public Long getUserId() { return userId; }
    public String getGameCode() { return gameCode; }
    public String getCurrency() { return currency; }
    public BigDecimal getBetAmount() { return betAmount; }
    public BigDecimal getPayoutAmount() { return payoutAmount; }
    public BigDecimal getMultiplier() { return multiplier; }
    public String getReelResultJson() { return reelResultJson; }
    public String getStatus() { return status; }
    public Long getDebitLedgerId() { return debitLedgerId; }
    public Long getCreditLedgerId() { return creditLedgerId; }
    public Instant getCreatedAt() { return createdAt; }
}
