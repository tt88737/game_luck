package com.tangluck.p1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_packages")
public class ProductPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_code", nullable = false)
    private String packageCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_amount", nullable = false)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false)
    private String priceCurrency;

    @Column(name = "gc_amount", nullable = false)
    private BigDecimal gcAmount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String provider;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "legal_approval_id")
    private String legalApprovalId;

    @Column(name = "sandbox_only", nullable = false)
    private boolean sandboxOnly;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProductPackage() {
    }

    public void update(String name, BigDecimal priceAmount, String priceCurrency, BigDecimal gcAmount, String status, String provider, Integer sortOrder, String legalApprovalId) {
        this.name = name;
        this.priceAmount = priceAmount;
        this.priceCurrency = priceCurrency;
        this.gcAmount = gcAmount;
        this.status = status;
        this.provider = provider;
        this.sortOrder = sortOrder;
        this.legalApprovalId = legalApprovalId;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public BigDecimal getGcAmount() {
        return gcAmount;
    }

    public boolean isSandboxOnly() {
        return sandboxOnly;
    }

    public String getStatus() {
        return status;
    }

    public String getProvider() {
        return provider;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getLegalApprovalId() {
        return legalApprovalId;
    }
}
