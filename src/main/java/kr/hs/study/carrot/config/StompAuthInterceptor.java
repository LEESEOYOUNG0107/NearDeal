package kr.hs.study.carrot.config;

import java.security.Principal;
import kr.hs.study.carrot.security.JwtProvider;
import kr.hs.study.carrot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// WebSocket(STOMP) 쪽의 "로그인 + 권한 검사". REST의 Spring Security 필터와 같은 역할
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String ERROR_QUEUE = "/user/queue/errors";
    private static final String BEARER = "Bearer ";

    private final ChatService chatService;
    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
            case SUBSCRIBE -> authorizeSubscribe(accessor);
            default -> { }
        }
        return message;
    }

    // CONNECT 프레임의 "Authorization: Bearer {토큰}" 헤더로 로그인 사용자를 확인한다. 실패하면 연결이 거부된다
    // ponytail: 토큰은 연결할 때 한 번만 검사한다. 연결을 유지한 채 토큰이 만료돼도 끊기지 않고, 다시 연결할 때 만료가 적용된다
    private Principal authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new MessageDeliveryException("로그인이 필요합니다. CONNECT 헤더에 Authorization: Bearer {토큰}을 보내세요.");
        }
        try {
            String memberId = String.valueOf(jwtProvider.parseMemberId(authorization.substring(BEARER.length()).trim()));
            return () -> memberId;
        } catch (ResponseStatusException e) {
            throw new MessageDeliveryException(e.getReason());
        }
    }

    // 자기가 참여한 방의 구독만 허용. 그렇지 않으면 다른 사람의 대화를 엿들을 수 있다
    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (user == null) {
            throw new MessageDeliveryException("연결 정보가 없습니다.");
        }
        if (ChatTopic.matches(destination)) {
            try {
                chatService.assertParticipant(ChatTopic.roomIdOf(destination), Long.valueOf(user.getName()));
            } catch (NumberFormatException e) {
                throw new MessageDeliveryException("잘못된 채팅방 경로입니다.");
            }
        } else if (!ERROR_QUEUE.equals(destination)) {
            throw new MessageDeliveryException("허용되지 않은 구독 경로입니다.");
        }
    }
}
