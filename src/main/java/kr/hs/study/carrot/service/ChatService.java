package kr.hs.study.carrot.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import kr.hs.study.carrot.dto.response.ChatMessageResponse;
import kr.hs.study.carrot.dto.response.ChatRoomDetail;
import kr.hs.study.carrot.dto.response.ChatRoomListItem;
import kr.hs.study.carrot.dto.response.ChatRoomResponse;
import kr.hs.study.carrot.entity.ChatMessage;
import kr.hs.study.carrot.entity.ChatRoom;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.repository.ChatMessageRepository;
import kr.hs.study.carrot.repository.ChatRoomRepository;
import kr.hs.study.carrot.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_CONTENT_LENGTH = 1000;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    // 화면 표시 이름. 기본은 "사용자 N"이고, 앱이 뜨면 MemberNames 가 회원 닉네임 조회로 바꿔 끼운다
    // (정적 메서드라 서비스 생성자를 건드리지 않고 알림/채팅/화면이 같은 이름을 쓰게 하려는 선택)
    private static volatile java.util.function.LongFunction<String> nameResolver = id -> "사용자 " + id;

    public static void useNameResolver(java.util.function.LongFunction<String> resolver) {
        nameResolver = resolver;
    }

    public static String nameOf(Long userId) {
        return nameResolver.apply(userId);
    }

    // ---- REST API / 공통 핵심 로직 ----

    // ponytail: 같은 구매자가 동시에 두 번 누르면 유니크 제약 때문에 한쪽이 500. 중복 방은 DB가 막으므로 데이터는 안전
    @Transactional
    public ChatRoomResponse createRoom(Long productId, Long buyerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다. id=" + productId));
        if (product.getSellerId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인이 등록한 상품에는 채팅방을 만들 수 없습니다.");
        }
        ChatRoom room = chatRoomRepository.findByProductIdAndBuyerId(productId, buyerId)
                .orElseGet(() -> chatRoomRepository.save(new ChatRoom(product, buyerId)));
        return ChatRoomResponse.from(room);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms(Long userId) {
        return chatRoomRepository.findAllByParticipant(userId).stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    // ponytail: 전체 메시지를 한 번에 반환. 대화가 길어지면 페이지/커서 방식으로 교체
    // 방의 대화를 가져오는 것은 "읽는 행위"이므로 상대가 보낸 메시지를 읽음 처리한다
    @Transactional
    public List<ChatMessageResponse> getMessages(Long roomId, Long userId) {
        findParticipatingRoom(roomId, userId);
        chatMessageRepository.markSeen(roomId, userId);
        notificationService.markChatRead(userId, roomId);
        return chatMessageRepository.findByChatRoomIdOrderByIdAsc(roomId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    // REST, 화면 폼, (이후) WebSocket 모두 이 메서드 하나로 저장한다. 입력 검증도 여기서 한 번만 한다
    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long userId, String content) {
        if (!StringUtils.hasText(content) || content.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지는 1~" + MAX_CONTENT_LENGTH + "자여야 합니다.");
        }
        ChatRoom room = findParticipatingRoom(roomId, userId);
        ChatMessageResponse saved = ChatMessageResponse.from(chatMessageRepository.save(new ChatMessage(room, userId, content)));
        notificationService.onChatMessage(room, userId, content);
        eventPublisher.publishEvent(new MessageSent(saved));
        return saved;
    }

    // 저장 완료를 알리는 이벤트. 실시간 방송(WebSocket)은 이 이벤트를 받아서 하므로 서비스는 WebSocket을 모른다
    public record MessageSent(ChatMessageResponse message) {
    }

    @Transactional(readOnly = true)
    public void assertParticipant(Long roomId, Long userId) {
        findParticipatingRoom(roomId, userId);
    }

    // ---- 화면(Thymeleaf)용 ----

    public Long openRoom(Long productId, Long userId) {
        return createRoom(productId, userId).id();
    }

    // ponytail: 방마다 마지막 메시지/안 읽은 수를 따로 조회(N+1). 방이 수십 개를 넘으면 한 번의 집계 쿼리로 교체
    @Transactional(readOnly = true)
    public List<ChatRoomListItem> rooms(Long userId) {
        // 메시지가 아직 없는 새 방은 맨 위, 나머지는 마지막 메시지가 최근인 순
        record Row(ChatRoom room, Optional<ChatMessage> last) {
            LocalDateTime activeAt() {
                return last.map(ChatMessage::getCreatedAt).orElse(LocalDateTime.MAX);
            }
        }
        return chatRoomRepository.findAllByParticipant(userId).stream()
                .map(r -> new Row(r, chatMessageRepository.findFirstByChatRoomIdOrderByIdDesc(r.getId())))
                .sorted(Comparator.comparing(Row::activeAt).reversed())
                .map(row -> {
                    ChatRoom r = row.room();
                    Product p = r.getProduct();
                    return new ChatRoomListItem(
                            r.getId(), p.getTitle(), p.getPrice(), p.getCategory(), p.getStatus(),
                            nameOf(peerOf(r, userId)),
                            row.last().map(ChatMessage::getContent).orElse(""),
                            row.last().map(ChatMessage::getCreatedAt).orElse(null),
                            chatMessageRepository.countByChatRoomIdAndSenderIdNotAndSeenFalse(r.getId(), userId));
                })
                .toList();
    }

    @Transactional
    public ChatRoomDetail room(Long roomId, Long userId) {
        ChatRoom room = findParticipatingRoom(roomId, userId);
        chatMessageRepository.markSeen(roomId, userId);
        notificationService.markChatRead(userId, roomId);
        List<ChatRoomDetail.Message> messages = chatMessageRepository.findByChatRoomIdOrderByIdAsc(roomId).stream()
                .map(m -> new ChatRoomDetail.Message(m.getContent(), m.getSenderId().equals(userId), m.isSeen(), m.getCreatedAt()))
                .toList();
        Product p = room.getProduct();
        return new ChatRoomDetail(
                room.getId(), p.getId(), p.getTitle(), p.getPrice(), p.getCategory(), p.getStatus(),
                p.getSellerId().equals(userId), nameOf(peerOf(room, userId)), messages);
    }

    public void send(Long roomId, Long userId, String content) {
        sendMessage(roomId, userId, content);
    }

    // ---- 내부 ----

    private Long peerOf(ChatRoom room, Long userId) {
        return room.getBuyerId().equals(userId) ? room.getSellerId() : room.getBuyerId();
    }

    private ChatRoom findParticipatingRoom(Long roomId, Long userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다. id=" + roomId));
        if (!room.hasParticipant(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 채팅방의 참여자만 접근할 수 있습니다.");
        }
        return room;
    }
}
