package kr.hs.study.carrot.controller;

import java.util.List;
import java.util.Map;
import kr.hs.study.carrot.dto.response.NotificationResponse;
import kr.hs.study.carrot.security.CurrentUserId;
import kr.hs.study.carrot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(notificationService.list(userId, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Long notificationId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(notificationService.markRead(notificationId, userId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllRead(userId)));
    }
}
