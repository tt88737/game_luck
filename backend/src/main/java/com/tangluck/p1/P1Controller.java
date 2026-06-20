package com.tangluck.p1;

import com.tangluck.admin.AdminOperatorContext;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tangluck.p1.P1Dtos.CreatePurchaseOrderRequest;
import static com.tangluck.p1.P1Dtos.CreateRedemptionRequest;
import static com.tangluck.p1.P1Dtos.KycApplicationRequest;
import static com.tangluck.p1.P1Dtos.KycStatusDto;
import static com.tangluck.p1.P1Dtos.MarkPurchaseOrderPaidRequest;
import static com.tangluck.p1.P1Dtos.P1OperationsDto;
import static com.tangluck.p1.P1Dtos.ProductPackageDto;
import static com.tangluck.p1.P1Dtos.PurchaseOrderDto;
import static com.tangluck.p1.P1Dtos.RedemptionDto;
import static com.tangluck.p1.P1Dtos.UpdateProductPackageRequest;

@RestController
@RequestMapping("/api/v1")
public class P1Controller {
    private final P1Service p1Service;

    public P1Controller(P1Service p1Service) {
        this.p1Service = p1Service;
    }

    @GetMapping("/purchase/packages")
    public List<ProductPackageDto> packages() {
        return p1Service.packages();
    }

    @GetMapping("/admin/product-packages")
    public List<ProductPackageDto> adminPackages(HttpServletRequest servletRequest) {
        AdminOperatorContext.from(servletRequest).require("package.read");
        return p1Service.adminPackages();
    }

    @PatchMapping("/admin/product-packages/{packageCode}")
    public ProductPackageDto updatePackage(@PathVariable String packageCode, @Valid @RequestBody UpdateProductPackageRequest request, HttpServletRequest servletRequest) {
        var operator = AdminOperatorContext.from(servletRequest);
        operator.require("package.write");
        return p1Service.updatePackage(packageCode, request, operator);
    }

    @PostMapping("/purchase/orders")
    public PurchaseOrderDto createOrder(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePurchaseOrderRequest request
    ) {
        return p1Service.createOrder(userId, idempotencyKey, request);
    }

    @GetMapping("/kyc/status")
    public KycStatusDto kycStatus(@RequestHeader("X-User-Id") Long userId) {
        return p1Service.kycStatus(userId);
    }

    @PostMapping("/kyc/applications")
    public KycStatusDto submitKyc(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody KycApplicationRequest request
    ) {
        return p1Service.submitKyc(userId, request);
    }

    @PostMapping("/redemptions")
    public RedemptionDto createRedemption(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateRedemptionRequest request
    ) {
        return p1Service.createRedemption(userId, idempotencyKey, request);
    }

    @GetMapping("/admin/p1/operations")
    public P1OperationsDto operations() {
        return p1Service.operations();
    }

    @PostMapping("/admin/purchase-orders/{orderId}/mark-paid")
    public PurchaseOrderDto markOrderPaid(@PathVariable String orderId, @RequestBody MarkPurchaseOrderPaidRequest request, HttpServletRequest servletRequest) {
        var operator = AdminOperatorContext.from(servletRequest);
        operator.require("order.settle");
        return p1Service.markOrderPaid(orderId, request, operator);
    }

    @PostMapping("/admin/kyc/{userId}/approve")
    public KycStatusDto approveKyc(@PathVariable Long userId, HttpServletRequest servletRequest) {
        var operator = AdminOperatorContext.from(servletRequest);
        operator.require("kyc.review");
        return p1Service.approveKyc(userId, operator);
    }
}
