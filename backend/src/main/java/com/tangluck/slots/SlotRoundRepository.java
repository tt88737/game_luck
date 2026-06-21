package com.tangluck.slots;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlotRoundRepository extends JpaRepository<SlotRound, Long> {
    Optional<SlotRound> findByIdempotencyKey(String idempotencyKey);

    Page<SlotRound> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<SlotRound> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
