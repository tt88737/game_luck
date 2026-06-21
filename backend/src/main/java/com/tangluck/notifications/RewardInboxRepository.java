package com.tangluck.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardInboxRepository extends JpaRepository<RewardInboxItem, Long> {
    List<RewardInboxItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RewardInboxItem> findTop100ByOrderByCreatedAtDesc();
}
