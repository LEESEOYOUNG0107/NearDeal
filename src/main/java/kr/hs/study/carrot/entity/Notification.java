package kr.hs.study.carrot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// productId/roomId는 클릭했을 때 이동할 곳. 상품이 삭제돼도 알림 기록은 남기므로 FK를 걸지 않는다
@Entity
@Table(name = "notification", indexes = @Index(columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    private Long productId;

    private Long roomId;

    // 알림을 일으킨 사람(찜한 사람, 메시지 보낸 사람, 상태를 바꾼 사람). 같은 사람의 중복 찜 알림을 막는 데 쓴다
    private Long actorId;

    @Column(name = "is_read", nullable = false)
    private boolean seen;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Notification(Long userId, NotificationType type, String message, Long productId, Long roomId, Long actorId) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.productId = productId;
        this.roomId = roomId;
        this.actorId = actorId;
        this.createdAt = LocalDateTime.now();
    }

    // 같은 방의 읽지 않은 채팅 알림을 새로 쌓지 않고 최신 내용으로 갱신
    public void refresh(String message) {
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public void markSeen() {
        this.seen = true;
    }
}
