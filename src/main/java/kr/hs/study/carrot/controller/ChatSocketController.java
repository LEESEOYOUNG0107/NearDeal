package kr.hs.study.carrot.controller;

import java.security.Principal;
import kr.hs.study.carrot.dto.request.ChatMessageRequest;
import kr.hs.study.carrot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

// 클라이언트가 /app/chat-rooms/{roomId}/messages 로 보낸 메시지를 받는다.
// 저장과 권한 검사는 ChatService가 하고, 저장이 커밋되면 ChatMessageBroadcaster가 방 전체에 방송한다
@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat-rooms/{roomId}/messages")
    public void send(@DestinationVariable Long roomId, ChatMessageRequest request, Principal principal) {
        chatService.sendMessage(roomId, Long.valueOf(principal.getName()), request.content());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleError(Exception e) {
        return e instanceof ResponseStatusException r ? r.getReason() : "메시지를 보내지 못했습니다.";
    }
}
