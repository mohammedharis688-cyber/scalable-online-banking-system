package com.systemdesign.banking.notification;

import com.systemdesign.banking.auth.CustomerPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal CustomerPrincipal principal) {
        List<Notification> list = notificationService.getCustomerNotifications(principal.getCustomerId());
        long unreadCount = notificationService.getUnreadCount(principal.getCustomerId());
        return ResponseEntity.ok(Map.of(
                "notifications", list,
                "unreadCount", unreadCount
        ));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable String notificationId,
            @AuthenticationPrincipal CustomerPrincipal principal) {
        notificationService.markAsRead(notificationId, principal.getCustomerId());
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal CustomerPrincipal principal) {
        notificationService.markAllAsRead(principal.getCustomerId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
