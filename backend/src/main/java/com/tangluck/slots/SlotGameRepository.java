package com.tangluck.slots;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlotGameRepository extends JpaRepository<SlotGame, Long> {
    List<SlotGame> findByStatusOrderBySortOrderAsc(String status);

    List<SlotGame> findAllByOrderBySortOrderAsc();

    Optional<SlotGame> findByGameCode(String gameCode);
}
