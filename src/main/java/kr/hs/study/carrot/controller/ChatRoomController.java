package kr.hs.study.carrot.controller;

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

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(
            @Valid @RequestBody ChatRoomCreateRequest request,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(chatService.createRoom(request.productId(), userId));
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(chatService.getMyRooms(userId));
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long roomId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(chatService.getMessages(roomId, userId));
    }

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageRequest request,
            @CurrentUserId Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendMessage(roomId, userId, request.content()));
    }
}
