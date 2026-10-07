package kr.hs.study.carrot.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.ProductStatus;

public record ChatRoomDetail(
        Long roomId,
        Long productId,
        String productTitle,
        Long productPrice,
        Category category,
        ProductStatus productStatus,
        boolean iAmSeller,
        String peerName,
        List<Message> messages
) {
    public record Message(String content, boolean mine, boolean seen, LocalDateTime createdAt) {
    }
}
