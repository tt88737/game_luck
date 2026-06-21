package com.tangluck.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "campaign_task_progress")
public class ActivityTaskProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_code", nullable = false)
    private String taskCode;

    @Column(nullable = false)
    private BigDecimal progress;

    @Column(name = "target_value", nullable = false)
    private BigDecimal targetValue;

    @Column(nullable = false)
    private String status;

    @Column(name = "reward_ledger_id")
    private Long rewardLedgerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    protected ActivityTaskProgress() {
    }

    public ActivityTaskProgress(Long userId, ActivityTask task, Instant now) {
        this.userId = userId;
        this.taskCode = task.getTaskCode();
        this.progress = BigDecimal.ZERO;
        this.targetValue = task.getTargetValue();
        this.status = "in_progress";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void increment(BigDecimal amount, Instant now) {
        if ("completed".equals(status)) return;
        this.progress = this.progress.add(amount);
        if (this.progress.compareTo(this.targetValue) >= 0) {
            this.status = "claimable";
        }
        this.updatedAt = now;
    }

    public void claim(Long ledgerId, Instant now) {
        this.status = "completed";
        this.rewardLedgerId = ledgerId;
        this.claimedAt = now;
        this.updatedAt = now;
    }

    public Long getUserId() { return userId; }
    public String getTaskCode() { return taskCode; }
    public BigDecimal getProgress() { return progress; }
    public BigDecimal getTargetValue() { return targetValue; }
    public String getStatus() { return status; }
    public Long getRewardLedgerId() { return rewardLedgerId; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getClaimedAt() { return claimedAt; }
}
