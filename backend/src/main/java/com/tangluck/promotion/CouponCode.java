package com.tangluck.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "coupon_codes")
public class CouponCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(name = "reward_policy_json", nullable = false)
    private String rewardPolicyJson;

    @Column(nullable = false)
    private String status;

    protected CouponCode() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getRewardPolicyJson() {
        return rewardPolicyJson;
    }

    public String getStatus() {
        return status;
    }
}
