package kr.hs.study.carrot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import kr.hs.study.carrot.dto.request.ChatMessageRequest;
import kr.hs.study.carrot.dto.request.ChatRoomCreateRequest;
import kr.hs.study.carrot.dto.response.ChatMessageResponse;
import kr.hs.study.carrot.dto.response.ChatRoomResponse;
import kr.hs.study.carrot.security.CurrentUserId;
import kr.hs.study.carrot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4. 채팅", description = "채팅방과 메시지(REST). 실시간 전송은 WebSocket(STOMP)이 맡고 저장은 같은 DB입니다.")
@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 만들기",
            description = "내가 구매자, 상품 판매자가 판매자가 됩니다. 같은 상품에 이미 방이 있으면 그 방을 돌려주고, 내 상품이면 400입니다.")
    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(
            @Valid @RequestBody ChatRoomCreateRequest request,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(chatService.createRoom(request.productId(), userId));
    }

    @Operation(summary = "내 채팅방 목록",
            description = "내가 구매자이거나 판매자인 방입니다.")
    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(chatService.getMyRooms(userId));
    }

    @Operation(summary = "메시지 목록",
            description = "방 참여자만 볼 수 있고(403), 상대가 보낸 메시지는 읽음 처리됩니다.")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(chatService.getMessages(roomId, userId));
    }

    @Operation(summary = "메시지 보내기",
            description = "방 참여자만 보낼 수 있고(403), 최대 1000자입니다. 구독 중인 상대에게 WebSocket으로도 전달되고 상대에게 알림이 갑니다.")
    @PostMapping("/{roomId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageRequest request,
            @CurrentUserId Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendMessage(roomId, userId, request.content()));
    }
}
