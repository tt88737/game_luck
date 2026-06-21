package com.tangluck.notifications;

import com.tangluck.admin.AdminAuditService;
import com.tangluck.admin.AdminOperatorContext;
import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import static com.tangluck.notifications.NotificationDtos.InboxItemDto;
import static com.tangluck.notifications.NotificationDtos.ManualGrantRequest;

@Service
public class NotificationService {
    private final RewardInboxRepository repository;
    private final WalletService walletService;
    private final AdminAuditService adminAuditService;
    private final Clock clock;

    public NotificationService(RewardInboxRepository repository, WalletService walletService, AdminAuditService adminAuditService) {
        this.repository = repository;
        this.walletService = walletService;
        this.adminAuditService = adminAuditService;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<InboxItemDto> playerInbox(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<InboxItemDto> adminInbox() {
        return repository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public InboxItemDto manualGrant(ManualGrantRequest request, AdminOperatorContext operator) {
        var saved = repository.save(new RewardInboxItem(request.userId(), request, clock.instant()));
        adminAuditService.write(operator, "reward_inbox_manual_grant", "reward_inbox", request.sourceId(), null, "{\"status\":\"claimable\"}", request.title());
        return toDto(saved);
    }

    @Transactional
    public InboxItemDto claim(Long userId, Long id) {
        var item = repository.findById(id)
                .filter(row -> row.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Reward inbox item does not exist.", Map.of("id", id)));
        if ("claimed".equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.CLAIM_DUPLICATED, "Reward already claimed.", Map.of("id", id));
        }
        if (!"claimable".equals(item.getStatus()) || (item.getExpiresAt() != null && item.getExpiresAt().isBefore(clock.instant()))) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Reward is not claimable.", Map.of("id", id));
        }
        var ledger = walletService.credit(userId, item.getRewardCurrency(), item.getRewardAmount(), "reward_inbox", String.valueOf(id), "reward:inbox:" + id);
        item.claim(ledger.ledgerId(), clock.instant());
        return toDto(repository.save(item));
    }

    @Transactional
    public InboxItemDto expire(Long id) {
        var item = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Reward inbox item does not exist.", Map.of("id", id)));
        item.expire();
        return toDto(repository.save(item));
    }

    private InboxItemDto toDto(RewardInboxItem item) {
        return new InboxItemDto(item.getId(), item.getUserId(), item.getTitle(), item.getMessage(), item.getRewardCurrency(), item.getRewardAmount(), item.getStatus(), item.getSourceType(), item.getSourceId(), item.getLedgerId(), item.getCreatedAt(), item.getExpiresAt(), item.getClaimedAt());
    }
}
