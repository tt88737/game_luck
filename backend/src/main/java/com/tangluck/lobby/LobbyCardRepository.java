package com.tangluck.lobby;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LobbyCardRepository extends JpaRepository<LobbyCard, Long> {
    Optional<LobbyCard> findByCardCode(String cardCode);

    List<LobbyCard> findByStatusOrderBySortOrderAsc(String status);
}
