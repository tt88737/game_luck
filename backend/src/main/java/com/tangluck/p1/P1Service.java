package com.tangluck.p1;

import com.tangluck.admin.AdminAuditService;
import com.tangluck.admin.AdminOperatorContext;
import com.tangluck.auth.UserRepository;
import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.compliance.ComplianceService;
import com.tangluck.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

@Service
public class P1Service {
    private final ProductPackageRepository packageRepository;
    private final PurchaseOrderRepository orderRepository;
    private final KycApplicationRepository kycRepository;
    private final RedemptionRequestRepository redemptionRepository;
    private final WalletService walletService;
    private final AdminAuditService adminAuditService;
    private final UserRepository userRepository;
    private final ComplianceService complianceService;
    private final Clock clock;

    public P1Service(
            ProductPackageRepository packageRepository,
            PurchaseOrderRepository orderRepository,
            KycApplicationRepository kycRepository,
            RedemptionRequestRepository redemptionRepository,
            WalletService walletService,
            AdminAuditService adminAuditService,
            UserRepository userRepository,
            ComplianceService complianceService
    ) {
        this.packageRepository = packageRepository;
        this.orderRepository = orderRepository;
        this.kycRepository = kycRepository;
        this.redemptionRepository = redemptionRepository;
        this.walletService = walletService;
        this.adminAuditService = adminAuditService;
        this.userRepository = userRepository;
        this.complianceService = complianceService;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<ProductPackageDto> packages() {
        return packageRepository.findByStatusOrderBySortOrderAscPriceAmountAsc("active").stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductPackageDto> adminPackages() {
        return packageRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public ProductPackageDto updatePackage(String packageCode, UpdateProductPackageRequest request, AdminOperatorContext operator) {
        var productPackage = packageRepository.findByPackageCode(packageCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_ALLOWED, "Product package is not available.", Map.of("packageCode", packageCode)));
        var before = packageJson(productPackage);
        productPackage.update(request.name(), request.priceAmount(), request.priceCurrency(), request.gcAmount(), request.status(), request.provider(), request.sortOrder(), request.legalApprovalId());
        var saved = packageRepository.save(productPackage);
        adminAuditService.write(operator, "product_package_update", "product_package", packageCode, before, packageJson(saved), null);
        return toDto(saved);
    }

    @Transactional
    public PurchaseOrderDto createOrder(Long userId, String idempotencyKey, CreatePurchaseOrderRequest request) {
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDto)
                .orElseGet(() -> createNewOrder(userId, idempotencyKey, request));
    }

    @Transactional(readOnly = true)
    public KycStatusDto kycStatus(Long userId) {
        return kycRepository.findByUserId(userId)
                .map(this::toDto)
                .orElse(new KycStatusDto(userId, "not_started", null, null, null));
    }

    @Transactional
    public KycStatusDto submitKyc(Long userId, KycApplicationRequest request) {
        var now = clock.instant();
        var application = kycRepository.findByUserId(userId)
                .map(existing -> {
                    existing.replace(request, now);
                    return existing;
                })
                .orElseGet(() -> new KycApplication(userId, request, now));
        return toDto(kycRepository.save(application));
    }

    @Transactional
    public KycStatusDto approveKyc(Long userId) {
        return approveKyc(userId, new AdminOperatorContext(1L, "ops_admin", java.util.Set.of("*"), "127.0.0.1"));
    }

    @Transactional
    public KycStatusDto approveKyc(Long userId, AdminOperatorContext operator) {
        var application = kycRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "KYC application does not exist.", Map.of("userId", userId)));
        var before = "{\"status\":\"" + application.getStatus() + "\"}";
        application.approve(clock.instant());
        var saved = kycRepository.save(application);
        adminAuditService.write(
                operator,
                "kyc_approve",
                "kyc_application",
                String.valueOf(userId),
                before,
                "{\"status\":\"" + saved.getStatus() + "\"}",
                saved.getReviewReason()
        );
        return toDto(saved);
    }

    @Transactional
    public RedemptionDto createRedemption(Long userId, String idempotencyKey, CreateRedemptionRequest request) {
        return redemptionRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDto)
                .orElseGet(() -> createNewRedemption(userId, idempotencyKey, request));
    }

    @Transactional(readOnly = true)
    public P1OperationsDto operations() {
        return new P1OperationsDto(
                orderRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::toDto).toList(),
                kycRepository.findTop50ByOrderByUpdatedAtDesc().stream().map(this::toDto).toList(),
                redemptionRepository.findTop50ByOrderByCreatedAtDesc().stream().map(this::toDto).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<KycStatusDto> adminKycApplications() {
        return kycRepository.findTop50ByOrderByUpdatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RedemptionDto> adminRedemptions() {
        return redemptionRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public PurchaseOrderDto markOrderPaid(String orderId, MarkPurchaseOrderPaidRequest request, AdminOperatorContext operator) {
        var order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_ALLOWED, "Purchase order does not exist.", Map.of("orderId", orderId)));
        if ("paid".equals(order.getStatus())) {
            return toDto(order);
        }
        var before = orderJson(order);
        var ledger = walletService.credit(order.getUserId(), "GC", order.getGcAmount(), "purchase", order.getOrderId(), "purchase:" + order.getOrderId());
        order.markPaid(request.providerReference(), ledger.ledgerId(), clock.instant());
        var saved = orderRepository.save(order);
        adminAuditService.write(operator, "purchase_order_mark_paid", "purchase_order", orderId, before, orderJson(saved), request.providerReference());
        return toDto(saved);
    }

    private PurchaseOrderDto createNewOrder(Long userId, String idempotencyKey, CreatePurchaseOrderRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "User does not exist.", Map.of("userId", userId)));
        complianceService.requireFeatureAllowed(user.getCountryCode(), user.getStateCode(), "purchase");
        var productPackage = packageRepository.findByPackageCodeAndStatus(request.packageCode(), "active")
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_ALLOWED, "Product package is not available.", Map.of("packageCode", request.packageCode())));
        var order = orderRepository.save(new PurchaseOrder("ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), userId, productPackage, idempotencyKey, clock.instant()));
        return toDto(orderRepository.save(order));
    }

    private RedemptionDto createNewRedemption(Long userId, String idempotencyKey, CreateRedemptionRequest request) {
        var kyc = kycRepository.findByUserId(userId);
        if (kyc.isEmpty() || !"approved".equals(kyc.get().getStatus())) {
            throw new BusinessException(ErrorCode.KYC_REQUIRED, "KYC approval is required before redemption.", Map.of("userId", userId));
        }
        var redemption = redemptionRepository.save(new RedemptionRequest("red_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16), userId, request.scAmount(), request.method(), idempotencyKey, clock.instant()));
        var ledger = walletService.freeze(userId, "SC", request.scAmount(), "redemption", redemption.getRedemptionId(), "redemption:freeze:" + redemption.getRedemptionId());
        redemption.attachFreezeLedger(ledger.ledgerId(), clock.instant());
        return toDto(redemptionRepository.save(redemption));
    }

    private ProductPackageDto toDto(ProductPackage productPackage) {
        return new ProductPackageDto(
                productPackage.getPackageCode(),
                productPackage.getName(),
                productPackage.getPriceAmount(),
                productPackage.getPriceCurrency(),
                productPackage.getGcAmount(),
                productPackage.isSandboxOnly(),
                productPackage.getStatus(),
                productPackage.getProvider(),
                productPackage.getSortOrder(),
                productPackage.getLegalApprovalId()
        );
    }

    private PurchaseOrderDto toDto(PurchaseOrder order) {
        return new PurchaseOrderDto(
                order.getOrderId(),
                order.getUserId(),
                order.getPackageCode(),
                order.getPriceAmount(),
                order.getPriceCurrency(),
                order.getStatus(),
                order.getProvider(),
                "GC",
                "paid".equals(order.getStatus()) ? order.getGcAmount() : java.math.BigDecimal.ZERO,
                order.getCreatedAt()
        );
    }

    private String packageJson(ProductPackage productPackage) {
        return "{\"status\":\"" + productPackage.getStatus() + "\",\"provider\":\"" + productPackage.getProvider() + "\"}";
    }

    private String orderJson(PurchaseOrder order) {
        return "{\"status\":\"" + order.getStatus() + "\",\"provider\":\"" + order.getProvider() + "\"}";
    }

    private KycStatusDto toDto(KycApplication application) {
        return new KycStatusDto(
                application.getUserId(),
                application.getStatus(),
                application.getLegalName(),
                application.getReviewReason(),
                application.getUpdatedAt()
        );
    }

    private RedemptionDto toDto(RedemptionRequest redemption) {
        return new RedemptionDto(
                redemption.getRedemptionId(),
                redemption.getUserId(),
                redemption.getScAmount(),
                redemption.getMethod(),
                redemption.getStatus(),
                redemption.isSandboxOnly(),
                redemption.getCreatedAt()
        );
    }
}
