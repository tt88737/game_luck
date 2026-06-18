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

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "state_code", nullable = false)
    private String stateCode;

    @Column(name = "registration_allowed", nullable = false)
    private boolean registrationAllowed;

    @Column(name = "sc_grant_allowed", nullable = false)
    private boolean scGrantAllowed;

    @Column(nullable = false)
    private String status;

    protected ComplianceRegion() {
    }

    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }

    public boolean isScGrantAllowed() {
        return scGrantAllowed;
    }

    public String getStatus() {
        return status;
    }
}
