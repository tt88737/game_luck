package com.tangluck.p1;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycApplicationRepository extends JpaRepository<KycApplication, Long> {
    Optional<KycApplication> findByUserId(Long userId);

    List<KycApplication> findTop50ByOrderByUpdatedAtDesc();
}
