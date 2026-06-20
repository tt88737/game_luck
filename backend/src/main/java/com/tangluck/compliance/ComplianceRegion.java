package com.tangluck.compliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "compliance_regions")
public class ComplianceRegion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_code", nullable = false, columnDefinition = "char(2)")
    private String countryCode;

    @Column(name = "state_code", nullable = false)
    private String stateCode;

    @Column(name = "registration_allowed", nullable = false)
    private boolean registrationAllowed;

    @Column(name = "game_allowed", nullable = false)
    private boolean gameAllowed;

    @Column(name = "purchase_allowed", nullable = false)
    private boolean purchaseAllowed;

    @Column(name = "sc_grant_allowed", nullable = false)
    private boolean scGrantAllowed;

    @Column(name = "redemption_allowed", nullable = false)
    private boolean redemptionAllowed;

    @Column(name = "amoe_allowed", nullable = false)
    private boolean amoeAllowed;

    @Column(name = "requires_legal_review", nullable = false)
    private boolean requiresLegalReview;

    @Column(nullable = false)
    private String status;

    @Column(name = "legal_approval_id")
    private String legalApprovalId;

    protected ComplianceRegion() {
    }

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public boolean isGameAllowed() {
        return gameAllowed;
    }

    public boolean isPurchaseAllowed() {
        return purchaseAllowed;
    }

    public boolean isScGrantAllowed() {
        return scGrantAllowed;
    }

    public boolean isRedemptionAllowed() {
        return redemptionAllowed;
    }

    public boolean isAmoeAllowed() {
        return amoeAllowed;
    }

    public boolean isRequiresLegalReview() {
        return requiresLegalReview;
    }

    public String getStatus() {
        return status;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getStateCode() {
        return stateCode;
    }

    public String getLegalApprovalId() {
        return legalApprovalId;
    }

    public void update(
            boolean registrationAllowed,
            boolean gameAllowed,
            boolean purchaseAllowed,
            boolean scGrantAllowed,
            boolean redemptionAllowed,
            boolean amoeAllowed,
            boolean requiresLegalReview,
            String status,
            String legalApprovalId
    ) {
        this.registrationAllowed = registrationAllowed;
        this.gameAllowed = gameAllowed;
        this.purchaseAllowed = purchaseAllowed;
        this.scGrantAllowed = scGrantAllowed;
        this.redemptionAllowed = redemptionAllowed;
        this.amoeAllowed = amoeAllowed;
        this.requiresLegalReview = requiresLegalReview;
        this.status = status;
        this.legalApprovalId = legalApprovalId;
    }
}
