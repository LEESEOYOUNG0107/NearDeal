package kr.hs.study.carrot.config;

import kr.hs.study.carrot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// 메시지가 DB에 커밋된 뒤에만 방송한다(저장이 롤백되면 방송되지 않음). REST/화면 폼/WebSocket 어디서 보내든 동일하게 동작
@Component
@RequiredArgsConstructor
public class ChatMessageBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener
    public void onMessageSent(ChatService.MessageSent event) {
        messagingTemplate.convertAndSend(ChatTopic.of(event.message().chatRoomId()), event.message());
    }
}
