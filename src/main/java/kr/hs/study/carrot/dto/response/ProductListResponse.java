package kr.hs.study.carrot.dto.response;

import java.time.LocalDateTime;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.entity.ProductStatus;

public record ProductListResponse(
        Long id,
        String title,
        Long price,
        Category category,
        String region,
        ProductStatus status,
        int viewCount,
        LocalDateTime createdAt
) {
    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getCategory(),
                product.getRegion(),
                product.getStatus(),
                product.getViewCount(),
                product.getCreatedAt()
        );
    }
}
