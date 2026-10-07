package kr.hs.study.carrot.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import kr.hs.study.carrot.dto.response.NotificationResponse;
import kr.hs.study.carrot.entity.ChatRoom;
import kr.hs.study.carrot.entity.Notification;
import kr.hs.study.carrot.entity.NotificationType;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.entity.ProductStatus;
import kr.hs.study.carrot.repository.ChatRoomRepository;
import kr.hs.study.carrot.repository.FavoriteRepository;
import kr.hs.study.carrot.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 알림 생성은 찜/채팅/상태 변경과 같은 트랜잭션에서 호출된다(본 작업이 롤백되면 알림도 남지 않음)
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int PREVIEW_LENGTH = 40;

    private final NotificationRepository notificationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final FavoriteRepository favoriteRepository;

    // ---- 알림 생성 ----

    // 내 상품이 찜됨 -> 판매자에게. 판매자 본인이 찜한 경우, 같은 사람이 찜/취소를 반복하는 경우는 알리지 않는다
    @Transactional
    public void onFavorited(Product product, Long actorId) {
        Long sellerId = product.getSellerId();
        if (sellerId.equals(actorId) || notificationRepository
                .existsByUserIdAndTypeAndProductIdAndActorId(sellerId, NotificationType.FAVORITE, product.getId(), actorId)) {
            return;
        }
        notificationRepository.save(new Notification(sellerId, NotificationType.FAVORITE,
                ChatService.nameOf(actorId) + "님이 '" + product.getTitle() + "' 상품을 찜했어요", product.getId(), null, actorId));
    }

    // 새 채팅 메시지 -> 상대방에게. 같은 방의 읽지 않은 알림이 있으면 새로 쌓지 않고 최신 내용으로 갱신
    @Transactional
    public void onChatMessage(ChatRoom room, Long senderId, String content) {
        Long receiverId = room.getBuyerId().equals(senderId) ? room.getSellerId() : room.getBuyerId();
        String preview = content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) + "…" : content;
        String message = ChatService.nameOf(senderId) + "님의 새 메시지: " + preview;
        notificationRepository.findFirstByUserIdAndTypeAndRoomIdAndSeenFalse(receiverId, NotificationType.CHAT_MESSAGE, room.getId())
                .ifPresentOrElse(
                        existing -> existing.refresh(message),
                        () -> notificationRepository.save(new Notification(
                                receiverId, NotificationType.CHAT_MESSAGE, message, room.getProduct().getId(), room.getId(), senderId)));
    }

    // 거래 상태 변경 -> 그 상품에 채팅을 건 구매자 + 찜한 사람에게. 변경한 사람 본인은 제외
    @Transactional
    public void onStatusChanged(Product product, Long actorId) {
        Set<Long> receivers = new LinkedHashSet<>();
        chatRoomRepository.findByProductId(product.getId()).forEach(room -> receivers.add(room.getBuyerId()));
        receivers.addAll(favoriteRepository.findUserIdsByProductId(product.getId()));
        receivers.remove(actorId);

        String message = "'" + product.getTitle() + "' 상품이 " + statusPhrase(product.getStatus()) + " 변경됐어요";
        receivers.forEach(userId -> notificationRepository.save(
                new Notification(userId, NotificationType.STATUS_CHANGE, message, product.getId(), null, actorId)));
    }

    private String statusPhrase(ProductStatus status) {
        return switch (status) {
            case SALE -> "판매중으로";
            case RESERVED -> "예약중으로";
            case SOLD_OUT -> "거래완료로";
        };
    }

    // 채팅방을 열어 대화를 읽으면 그 방의 채팅 알림도 읽음 처리
    @Transactional
    public void markChatRead(Long userId, Long roomId) {
        notificationRepository.markRoomSeen(userId, NotificationType.CHAT_MESSAGE, roomId);
    }

    // ---- REST 조회/읽음 ----

    // ponytail: 최근 50개만 반환. 더 필요하면 페이지 처리
    @Transactional(readOnly = true)
    public List<NotificationResponse> list(Long userId, boolean unreadOnly) {
        List<Notification> found = unreadOnly
                ? notificationRepository.findTop50ByUserIdAndSeenFalseOrderByIdDesc(userId)
                : notificationRepository.findTop50ByUserIdOrderByIdDesc(userId);
        return found.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndSeenFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다. id=" + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 알림만 읽음 처리할 수 있습니다.");
        }
        notification.markSeen();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllSeen(userId);
    }
}
