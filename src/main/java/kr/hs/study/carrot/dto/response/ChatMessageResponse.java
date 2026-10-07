package kr.hs.study.carrot.dto.response;

import java.time.LocalDateTime;
import kr.hs.study.carrot.entity.ChatMessage;

public record ChatMessageResponse(
        Long id,
        Long chatRoomId,
        Long senderId,
        String content,
        boolean seen,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderId(),
                message.getContent(),
                message.isSeen(),
                message.getCreatedAt()
        );
    }
}
