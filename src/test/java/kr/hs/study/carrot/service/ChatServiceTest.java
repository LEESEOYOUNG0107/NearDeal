package kr.hs.study.carrot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.ChatMessage;
import kr.hs.study.carrot.entity.ChatRoom;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.repository.ChatMessageRepository;
import kr.hs.study.carrot.repository.ChatRoomRepository;
import kr.hs.study.carrot.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ProductRepository productRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock NotificationService notificationService;
    @InjectMocks ChatService chatService;

    private Product productOf(Long sellerId) {
        return Product.builder().sellerId(sellerId).title("아이폰").price(1000L)
                .category(Category.DIGITAL).description("설명").region("서울").build();
    }

    private void assertStatus(Throwable e, HttpStatus expected) {
        assertThat(e).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(expected));
    }

    @Test
    void 본인_상품에는_채팅방을_만들_수_없다() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(productOf(1L)));

        assertThatThrownBy(() -> chatService.createRoom(1L, 1L)).satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    void 이미_있는_방이면_새로_만들지_않고_기존_방을_반환한다() {
        Product product = productOf(1L);
        ChatRoom existing = new ChatRoom(product, 2L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(chatRoomRepository.findByProductIdAndBuyerId(10L, 2L)).thenReturn(Optional.of(existing));

        chatService.createRoom(10L, 2L);

        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    void 방의_참여자가_아니면_메시지를_보내거나_볼_수_없다() {
        ChatRoom room = new ChatRoom(productOf(1L), 2L);
        when(chatRoomRepository.findById(5L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.sendMessage(5L, 3L, "안녕")).satisfies(e -> assertStatus(e, HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> chatService.getMessages(5L, 3L)).satisfies(e -> assertStatus(e, HttpStatus.FORBIDDEN));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void 구매자와_판매자는_모두_메시지를_보낼_수_있다() {
        ChatRoom room = new ChatRoom(productOf(1L), 2L);
        when(chatRoomRepository.findById(5L)).thenReturn(Optional.of(room));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(chatService.sendMessage(5L, 2L, "구매자").senderId()).isEqualTo(2L);
        assertThat(chatService.sendMessage(5L, 1L, "판매자").senderId()).isEqualTo(1L);
        verify(eventPublisher, times(2)).publishEvent(any(ChatService.MessageSent.class));
    }

    @Test
    void 검증에_실패한_메시지는_저장도_방송도_되지_않는다() {
        assertThatThrownBy(() -> chatService.sendMessage(5L, 2L, "  ")).satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> chatService.sendMessage(5L, 2L, "가".repeat(1001))).satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        verify(chatMessageRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
