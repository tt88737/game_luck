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
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "package_code", nullable = false)
    private String packageCode;

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

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "ledger_id")
    private Long ledgerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PurchaseOrder() {
    }

    public PurchaseOrder(String orderId, Long userId, ProductPackage productPackage, String idempotencyKey, Instant now) {
        this.orderId = orderId;
        this.userId = userId;
        this.packageCode = productPackage.getPackageCode();
        this.priceAmount = productPackage.getPriceAmount();
        this.priceCurrency = productPackage.getPriceCurrency();
        this.gcAmount = productPackage.getGcAmount();
        this.status = "paid";
        this.provider = "sandbox";
        this.idempotencyKey = idempotencyKey;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getPackageCode() {
        return packageCode;
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

    public String getStatus() {
        return status;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void attachLedger(Long ledgerId, Instant now) {
        this.ledgerId = ledgerId;
        this.updatedAt = now;
    }
}
