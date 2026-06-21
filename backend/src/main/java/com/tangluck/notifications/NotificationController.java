package com.tangluck.notifications;

import com.tangluck.admin.AdminOperatorContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.tangluck.notifications.NotificationDtos.InboxItemDto;
import static com.tangluck.notifications.NotificationDtos.ManualGrantRequest;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/player/notifications")
    public List<InboxItemDto> playerInbox(@RequestHeader("X-User-Id") Long userId) {
        return notificationService.playerInbox(userId);
    }

    @PostMapping("/player/notifications/{id}/claim")
    public InboxItemDto claim(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return notificationService.claim(userId, id);
    }

    @GetMapping("/admin/notifications")
    public List<InboxItemDto> adminInbox(HttpServletRequest request) {
        var operator = AdminOperatorContext.from(request);
        operator.require("notification.read");
        return notificationService.adminInbox();
    }

    @PostMapping("/admin/notifications/manual-grant")
    public InboxItemDto manualGrant(HttpServletRequest request, @Valid @RequestBody ManualGrantRequest grantRequest) {
        var operator = AdminOperatorContext.from(request);
        operator.require("notification.write");
        return notificationService.manualGrant(grantRequest, operator);
    }

    @PostMapping("/admin/notifications/{id}/expire")
    public InboxItemDto expire(HttpServletRequest request, @PathVariable Long id) {
        var operator = AdminOperatorContext.from(request);
        operator.require("notification.write");
        return notificationService.expire(id);
    }
}
