package com.tangluck.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComplianceRegionRepository extends JpaRepository<ComplianceRegion, Long> {
    Optional<ComplianceRegion> findByCountryCodeAndStateCode(String countryCode, String stateCode);
}
