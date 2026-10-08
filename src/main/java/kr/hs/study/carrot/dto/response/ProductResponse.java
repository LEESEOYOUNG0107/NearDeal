package kr.hs.study.carrot.dto.response;

import java.time.LocalDateTime;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.entity.ProductStatus;
import kr.hs.study.carrot.service.ChatService;

public record ProductResponse(
        Long id,
        Long sellerId,
        String sellerNickname,
        String title,
        Long price,
        Category category,
        String description,
        ProductStatus status,
        String region,
        int viewCount,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                ChatService.nameOf(product.getSellerId()),
                product.getTitle(),
                product.getPrice(),
                product.getCategory(),
                product.getDescription(),
                product.getStatus(),
                product.getRegion(),
                product.getViewCount(),
                product.getCreatedAt()
        );
    }
}
