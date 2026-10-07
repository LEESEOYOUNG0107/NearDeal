package kr.hs.study.carrot.dto.response;

import kr.hs.study.carrot.entity.ChatRoom;

public record ChatRoomResponse(
        Long id,
        Long productId,
        String productTitle,
        Long sellerId,
        Long buyerId
) {
    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getProduct().getId(),
                room.getProduct().getTitle(),
                room.getSellerId(),
                room.getBuyerId()
        );
    }
}
