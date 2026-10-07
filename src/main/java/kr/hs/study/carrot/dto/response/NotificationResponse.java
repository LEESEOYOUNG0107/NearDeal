package kr.hs.study.carrot.dto.response;

import java.time.LocalDateTime;
import kr.hs.study.carrot.entity.Notification;
import kr.hs.study.carrot.entity.NotificationType;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        Long productId,
        Long roomId,
        boolean seen,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getProductId(), n.getRoomId(), n.isSeen(), n.getCreatedAt());
    }
}
