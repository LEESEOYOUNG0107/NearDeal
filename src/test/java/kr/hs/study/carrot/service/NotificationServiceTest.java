package kr.hs.study.carrot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.ChatRoom;
import kr.hs.study.carrot.entity.Notification;
import kr.hs.study.carrot.entity.NotificationType;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.repository.ChatRoomRepository;
import kr.hs.study.carrot.repository.FavoriteRepository;
import kr.hs.study.carrot.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock FavoriteRepository favoriteRepository;
    @InjectMocks NotificationService notificationService;

    private Product productOf(Long sellerId) {
        return Product.builder().sellerId(sellerId).title("아이폰").price(1000L)
                .category(Category.DIGITAL).description("설명").region("서울").build();
    }

    private List<Notification> savedNotifications(int times) {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(times)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void 내_상품이_다른_사람에게_찜되면_판매자에게_알림이_간다() {
        notificationService.onFavorited(productOf(1L), 2L);

        Notification n = savedNotifications(1).get(0);
        assertThat(n.getUserId()).isEqualTo(1L);
        assertThat(n.getType()).isEqualTo(NotificationType.FAVORITE);
    }

    @Test
    void 같은_사람이_찜_취소_후_다시_찜해도_알림은_한_번만_간다() {
        when(notificationRepository.existsByUserIdAndTypeAndProductIdAndActorId(any(), any(), any(), any()))
                .thenReturn(false, true);
        Product product = productOf(1L);

        notificationService.onFavorited(product, 2L);
        notificationService.onFavorited(product, 2L);

        savedNotifications(1);
    }

    @Test
    void 판매자_본인이_찜하면_알림이_가지_않는다() {
        notificationService.onFavorited(productOf(1L), 1L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void 채팅_메시지_알림은_상대방에게_가고_읽지_않은_알림이_있으면_갱신한다() {
        ChatRoom room = new ChatRoom(productOf(1L), 2L);
        Notification existing = new Notification(1L, NotificationType.CHAT_MESSAGE, "예전 내용", null, null, 2L);
        when(notificationRepository.findFirstByUserIdAndTypeAndRoomIdAndSeenFalse(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        notificationService.onChatMessage(room, 2L, "새 메시지입니다");

        assertThat(existing.getMessage()).contains("새 메시지입니다");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void 채팅_알림이_없으면_받는_사람에게_새로_만든다() {
        ChatRoom room = new ChatRoom(productOf(1L), 2L);
        when(notificationRepository.findFirstByUserIdAndTypeAndRoomIdAndSeenFalse(any(), any(), any()))
                .thenReturn(Optional.empty());

        notificationService.onChatMessage(room, 1L, "판매자 답장");

        assertThat(savedNotifications(1).get(0).getUserId()).isEqualTo(2L);
    }

    @Test
    void 상태_변경_알림은_채팅_구매자와_찜한_사람에게_중복없이_가고_변경한_본인은_제외된다() {
        Product product = productOf(1L);
        when(chatRoomRepository.findByProductId(any()))
                .thenReturn(List.of(new ChatRoom(product, 2L), new ChatRoom(product, 3L)));
        when(favoriteRepository.findUserIdsByProductId(any())).thenReturn(List.of(3L, 4L, 1L));

        notificationService.onStatusChanged(product, 1L);

        assertThat(savedNotifications(3)).extracting(Notification::getUserId).containsExactly(2L, 3L, 4L);
    }
}
