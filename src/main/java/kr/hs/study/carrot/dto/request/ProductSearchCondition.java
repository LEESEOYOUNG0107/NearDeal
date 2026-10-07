package kr.hs.study.carrot.dto.request;

import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.ProductStatus;

// 모든 필드 선택. null이면 해당 조건은 적용하지 않음
public record ProductSearchCondition(
        String keyword,
        Category category,
        Long minPrice,
        Long maxPrice,
        ProductStatus status,
        String region
) {
}
