package com.tangluck.p1;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByIdempotencyKey(String idempotencyKey);

    Optional<PurchaseOrder> findByOrderId(String orderId);

    List<PurchaseOrder> findTop50ByOrderByCreatedAtDesc();
}
