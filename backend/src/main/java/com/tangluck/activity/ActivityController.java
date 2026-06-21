package com.tangluck.activity;

import com.tangluck.admin.AdminOperatorContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tangluck.activity.ActivityDtos.ActivitySummaryDto;
import static com.tangluck.activity.ActivityDtos.AdminActivityDashboardDto;
import static com.tangluck.activity.ActivityDtos.TaskClaimDto;

@RestController
@RequestMapping("/api/v1")
public class ActivityController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/player/activity-summary")
    public ActivitySummaryDto summary(@RequestHeader("X-User-Id") Long userId) {
        return activityService.summary(userId);
    }

    @PostMapping("/player/tasks/{taskCode}/claim")
    public TaskClaimDto claim(@RequestHeader("X-User-Id") Long userId, @PathVariable String taskCode) {
        return activityService.claim(userId, taskCode);
    }

    @GetMapping("/admin/activity-dashboard")
    public AdminActivityDashboardDto adminDashboard(HttpServletRequest request) {
        var operator = AdminOperatorContext.from(request);
        operator.require("activity.read");
        return activityService.adminDashboard();
    }
}
