package com.tangluck.activity;

import com.tangluck.common.api.BusinessException;
import com.tangluck.common.api.ErrorCode;
import com.tangluck.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tangluck.activity.ActivityDtos.ActivitySummaryDto;
import static com.tangluck.activity.ActivityDtos.ActivityTaskDto;
import static com.tangluck.activity.ActivityDtos.AdminActivityDashboardDto;
import static com.tangluck.activity.ActivityDtos.AdminTaskMetricDto;
import static com.tangluck.activity.ActivityDtos.TaskClaimDto;

@Service
public class ActivityService {
    private final ActivityTaskRepository taskRepository;
    private final ActivityTaskProgressRepository progressRepository;
    private final WalletService walletService;
    private final Clock clock;

    public ActivityService(ActivityTaskRepository taskRepository, ActivityTaskProgressRepository progressRepository, WalletService walletService) {
        this.taskRepository = taskRepository;
        this.progressRepository = progressRepository;
        this.walletService = walletService;
        this.clock = Clock.systemUTC();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSpin(Long userId, BigDecimal betAmount, BigDecimal payoutAmount) {
        for (var task : taskRepository.findByStatusOrderBySortOrderAsc("active")) {
            var increment = switch (task.getTargetType()) {
                case "spin_count" -> BigDecimal.ONE;
                case "bet_amount" -> betAmount;
                case "win_amount" -> payoutAmount;
                default -> BigDecimal.ZERO;
            };
            if (increment.compareTo(BigDecimal.ZERO) <= 0) continue;
            var progress = progressRepository.findByUserIdAndTaskCode(userId, task.getTaskCode())
                    .orElseGet(() -> new ActivityTaskProgress(userId, task, clock.instant()));
            progress.increment(increment, clock.instant());
            progressRepository.save(progress);
        }
    }

    @Transactional(readOnly = true)
    public ActivitySummaryDto summary(Long userId) {
        var progressByCode = progressRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(ActivityTaskProgress::getTaskCode, Function.identity()));
        var tasks = taskRepository.findByStatusOrderBySortOrderAsc("active").stream()
                .map(task -> toDto(task, progressByCode.get(task.getTaskCode())))
                .toList();
        return new ActivitySummaryDto(tasks, tasks.stream().filter(item -> "claimable".equals(item.status())).count());
    }

    @Transactional
    public TaskClaimDto claim(Long userId, String taskCode) {
        var task = taskRepository.findByTaskCodeAndStatus(taskCode, "active")
                .orElseThrow(() -> new BusinessException(ErrorCode.CAMPAIGN_NOT_ACTIVE, "Task is not active.", Map.of("taskCode", taskCode)));
        var progress = progressRepository.findByUserIdAndTaskCode(userId, taskCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, "Task is not completed.", Map.of("taskCode", taskCode)));
        if ("completed".equals(progress.getStatus())) {
            throw new BusinessException(ErrorCode.CLAIM_DUPLICATED, "Task reward already claimed.", Map.of("taskCode", taskCode));
        }
        if (!"claimable".equals(progress.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Task is not completed.", Map.of("taskCode", taskCode));
        }
        var ledger = walletService.credit(userId, task.getRewardCurrency(), task.getRewardAmount(), "activity_task", taskCode, "activity:task:" + userId + ":" + taskCode);
        progress.claim(ledger.ledgerId(), clock.instant());
        progressRepository.save(progress);
        return new TaskClaimDto(taskCode, progress.getStatus(), task.getRewardCurrency(), task.getRewardAmount(), ledger.ledgerId());
    }

    @Transactional(readOnly = true)
    public AdminActivityDashboardDto adminDashboard() {
        var tasks = taskRepository.findByStatusOrderBySortOrderAsc("active");
        var progress = progressRepository.findAll();
        var gcGranted = progress.stream()
                .filter(item -> "completed".equals(item.getStatus()))
                .map(item -> tasks.stream().filter(task -> task.getTaskCode().equals(item.getTaskCode())).findFirst().map(ActivityTask::getRewardAmount).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var metrics = tasks.stream()
                .map(task -> new AdminTaskMetricDto(
                        task.getTaskCode(),
                        task.getName(),
                        task.getTargetType(),
                        progress.stream().filter(item -> item.getTaskCode().equals(task.getTaskCode()) && "completed".equals(item.getStatus())).count(),
                        task.getRewardAmount()
                ))
                .toList();
        var participants = progress.stream().map(ActivityTaskProgress::getUserId).distinct().count();
        var completed = progress.stream().filter(item -> "completed".equals(item.getStatus())).count();
        return new AdminActivityDashboardDto(participants, completed, gcGranted, metrics);
    }

    private ActivityTaskDto toDto(ActivityTask task, ActivityTaskProgress progress) {
        return new ActivityTaskDto(
                task.getTaskCode(),
                task.getName(),
                task.getTargetType(),
                progress == null ? BigDecimal.ZERO : progress.getProgress(),
                task.getTargetValue(),
                progress == null ? "in_progress" : progress.getStatus(),
                task.getRewardCurrency(),
                task.getRewardAmount()
        );
    }
}
