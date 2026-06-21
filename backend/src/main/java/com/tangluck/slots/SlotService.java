package com.tangluck.slots;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tangluck.admin.AdminAuditService;
import com.tangluck.admin.AdminOperatorContext;
import com.tangluck.activity.ActivityService;
import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.wallet.WalletService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tangluck.slots.SlotDtos.SlotGameDto;
import static com.tangluck.slots.SlotDtos.SlotRoundDto;
import static com.tangluck.slots.SlotDtos.SlotRoundPage;
import static com.tangluck.slots.SlotDtos.SpinRequest;
import static com.tangluck.slots.SlotDtos.UpdateSlotGameRequest;

@Service
public class SlotService {
    private static final List<List<String>> WIN_REELS = List.of(
            List.of("coin", "seven", "coin"),
            List.of("coin", "seven", "coin"),
            List.of("coin", "seven", "coin"),
            List.of("coin", "seven", "coin"),
            List.of("coin", "seven", "coin")
    );
    private static final List<List<String>> LOSE_REELS = List.of(
            List.of("coin", "bar", "lemon"),
            List.of("cherry", "seven", "coin"),
            List.of("lemon", "bar", "coin"),
            List.of("coin", "cherry", "bar"),
            List.of("bar", "coin", "lemon")
    );

    private final SlotGameRepository gameRepository;
    private final SlotRoundRepository roundRepository;
    private final WalletService walletService;
    private final ActivityService activityService;
    private final AdminAuditService adminAuditService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SlotService(SlotGameRepository gameRepository, SlotRoundRepository roundRepository, WalletService walletService, ActivityService activityService, AdminAuditService adminAuditService, ObjectMapper objectMapper) {
        this.gameRepository = gameRepository;
        this.roundRepository = roundRepository;
        this.walletService = walletService;
        this.activityService = activityService;
        this.adminAuditService = adminAuditService;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<SlotGameDto> activeGames() {
        return gameRepository.findByStatusOrderBySortOrderAsc("active").stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SlotGameDto> adminGames() {
        return gameRepository.findAllByOrderBySortOrderAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public SlotRoundDto spin(Long userId, String gameCode, String idempotencyKey, SpinRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Idempotency key is required.", Map.of());
        }
        return roundRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDto)
                .orElseGet(() -> createRound(userId, gameCode, idempotencyKey, request));
    }

    @Transactional(readOnly = true)
    public SlotRoundPage playerRounds(Long userId, int page, int pageSize) {
        var pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);
        var rounds = roundRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return new SlotRoundPage(rounds.getContent().stream().map(this::toDto).toList(), page, pageSize, rounds.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SlotRoundPage adminRounds(int page, int pageSize) {
        var pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);
        var rounds = roundRepository.findAllByOrderByCreatedAtDesc(pageable);
        return new SlotRoundPage(rounds.getContent().stream().map(this::toDto).toList(), page, pageSize, rounds.getTotalElements());
    }

    @Transactional
    public SlotGameDto updateGame(String gameCode, UpdateSlotGameRequest request, AdminOperatorContext operator) {
        var game = gameRepository.findByGameCode(gameCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Slot game does not exist.", Map.of("gameCode", gameCode)));
        var before = gameJson(game);
        game.update(request, clock.instant());
        var saved = gameRepository.save(game);
        adminAuditService.write(operator, "slot_game_update", "slot_game", gameCode, before, gameJson(saved), request.legalApprovalId());
        return toDto(saved);
    }

    private SlotRoundDto createRound(Long userId, String gameCode, String idempotencyKey, SpinRequest request) {
        var game = gameRepository.findByGameCode(gameCode)
                .filter(item -> "active".equals(item.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Slot game is not available.", Map.of("gameCode", gameCode)));
        if (!"GC".equals(request.currency()) || !game.getCurrency().equals(request.currency())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Only GC play is available.", Map.of("currency", request.currency()));
        }
        if (request.betAmount().compareTo(game.getMinBet()) < 0 || request.betAmount().compareTo(game.getMaxBet()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Bet amount is outside game limits.", Map.of("gameCode", gameCode));
        }

        var multiplier = request.betAmount().compareTo(new BigDecimal("10.0000")) == 0 ? new BigDecimal("2.0000") : BigDecimal.ZERO.setScale(4);
        var payout = request.betAmount().multiply(multiplier);
        var reels = multiplier.compareTo(BigDecimal.ZERO) > 0 ? WIN_REELS : LOSE_REELS;
        var round = roundRepository.save(new SlotRound(
                "round_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                userId,
                gameCode,
                request.currency(),
                request.betAmount(),
                payout,
                multiplier,
                toJson(reels),
                "settled",
                idempotencyKey,
                clock.instant()
        ));
        var debit = walletService.debit(userId, "GC", request.betAmount(), "slot_bet", round.getRoundId(), "slot:bet:" + idempotencyKey);
        var creditLedgerId = payout.compareTo(BigDecimal.ZERO) > 0
                ? walletService.credit(userId, "GC", payout, "slot_payout", round.getRoundId(), "slot:payout:" + idempotencyKey).ledgerId()
                : null;
        round.attachLedgers(debit.ledgerId(), creditLedgerId);
        activityService.recordSpin(userId, request.betAmount(), payout);
        return toDto(roundRepository.save(round));
    }

    private SlotGameDto toDto(SlotGame game) {
        return new SlotGameDto(game.getGameCode(), game.getName(), game.getStatus(), game.getReelCount(), game.getRowCount(), game.getMinBet(), game.getMaxBet(), game.getCurrency(), game.getSortOrder(), game.getLegalApprovalId());
    }

    private SlotRoundDto toDto(SlotRound round) {
        return new SlotRoundDto(round.getRoundId(), round.getUserId(), round.getGameCode(), round.getCurrency(), round.getBetAmount(), round.getPayoutAmount(), round.getMultiplier(), readReels(round.getReelResultJson()), round.getStatus(), round.getDebitLedgerId(), round.getCreditLedgerId(), round.getCreatedAt());
    }

    private String gameJson(SlotGame game) {
        return "{\"status\":\"" + game.getStatus() + "\",\"minBet\":\"" + game.getMinBet() + "\",\"maxBet\":\"" + game.getMaxBet() + "\"}";
    }

    private String toJson(List<List<String>> reels) {
        try {
            return objectMapper.writeValueAsString(reels);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Could not serialize slot reels.", Map.of());
        }
    }

    private List<List<String>> readReels(String value) {
        try {
            var node = objectMapper.readTree(value);
            var payload = node.isTextual() ? node.asText() : value;
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Could not read slot reels.", Map.of());
        }
    }
}
