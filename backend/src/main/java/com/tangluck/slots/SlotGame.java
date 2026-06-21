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
@Table(name = "slot_games")
public class SlotGame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_code", nullable = false)
    private String gameCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status;

    @Column(name = "reel_count", nullable = false)
    private int reelCount;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "min_bet", nullable = false)
    private BigDecimal minBet;

    @Column(name = "max_bet", nullable = false)
    private BigDecimal maxBet;

    @Column(nullable = false)
    private String currency;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "legal_approval_id")
    private String legalApprovalId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SlotGame() {
    }

    public String getGameCode() { return gameCode; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public int getReelCount() { return reelCount; }
    public int getRowCount() { return rowCount; }
    public BigDecimal getMinBet() { return minBet; }
    public BigDecimal getMaxBet() { return maxBet; }
    public String getCurrency() { return currency; }
    public int getSortOrder() { return sortOrder; }
    public String getLegalApprovalId() { return legalApprovalId; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(SlotDtos.UpdateSlotGameRequest request, Instant now) {
        this.name = request.name();
        this.status = request.status();
        this.minBet = request.minBet();
        this.maxBet = request.maxBet();
        this.sortOrder = request.sortOrder();
        this.legalApprovalId = request.legalApprovalId();
        this.updatedAt = now;
    }
}
