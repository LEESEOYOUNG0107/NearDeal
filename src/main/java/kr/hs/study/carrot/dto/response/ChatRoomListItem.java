package kr.hs.study.carrot.dto.response;

import java.time.LocalDateTime;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.ProductStatus;

public record ChatRoomListItem(
        Long roomId,
        String productTitle,
        Long productPrice,
        Category category,
        ProductStatus productStatus,
        String peerName,
        String lastMessage,
        LocalDateTime lastAt,
        long unread
) {
}
