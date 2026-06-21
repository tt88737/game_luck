package com.tangluck.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityTaskProgressRepository extends JpaRepository<ActivityTaskProgress, Long> {
    Optional<ActivityTaskProgress> findByUserIdAndTaskCode(Long userId, String taskCode);

    List<ActivityTaskProgress> findByUserId(Long userId);

    long countDistinctByUserIdIsNotNull();

    long countByStatus(String status);
}
