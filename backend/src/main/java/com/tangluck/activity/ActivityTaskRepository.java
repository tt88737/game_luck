package com.tangluck.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityTaskRepository extends JpaRepository<ActivityTask, Long> {
    List<ActivityTask> findByStatusOrderBySortOrderAsc(String status);

    Optional<ActivityTask> findByTaskCodeAndStatus(String taskCode, String status);
}
