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
@Table(name = "campaign_tasks")
public class ActivityTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_code", nullable = false)
    private String taskCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_value", nullable = false)
    private BigDecimal targetValue;

    @Column(name = "reward_currency", nullable = false)
    private String rewardCurrency;

    @Column(name = "reward_amount", nullable = false)
    private BigDecimal rewardAmount;

    @Column(nullable = false)
    private String status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    protected ActivityTask() {
    }

    public String getTaskCode() { return taskCode; }
    public String getName() { return name; }
    public String getTargetType() { return targetType; }
    public BigDecimal getTargetValue() { return targetValue; }
    public String getRewardCurrency() { return rewardCurrency; }
    public BigDecimal getRewardAmount() { return rewardAmount; }
    public String getStatus() { return status; }
    public int getSortOrder() { return sortOrder; }
}
