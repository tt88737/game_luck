package com.tangluck.p1;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RedemptionRequestRepository extends JpaRepository<RedemptionRequest, Long> {
    Optional<RedemptionRequest> findByIdempotencyKey(String idempotencyKey);

    Optional<RedemptionRequest> findByRedemptionId(String redemptionId);

    List<RedemptionRequest> findTop50ByOrderByCreatedAtDesc();
}
