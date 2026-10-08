package kr.hs.study.carrot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "5. 알림", description = "찜, 새 채팅 메시지, 거래 상태 변경 알림")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록",
            description = "최신순입니다. unreadOnly=true 이면 안 읽은 알림만 돌려줍니다.")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(notificationService.list(userId, unreadOnly));
    }

    @Operation(summary = "안 읽은 알림 개수")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(userId)));
    }

    @Operation(summary = "알림 읽음 처리",
            description = "없으면 404, 남의 알림이면 403입니다.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Long notificationId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(notificationService.markRead(notificationId, userId));
    }

    @Operation(summary = "알림 모두 읽음 처리",
            description = "읽음 처리된 개수를 돌려줍니다.")
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllRead(userId)));
    }
}
