package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// condition은 사용자가 직접 쓰는 상태 설명("거의 새것"). ProductStatus(판매중/예약중/판매완료)와 다름
public record ProductDescriptionRequest(
        @io.swagger.v3.oas.annotations.media.Schema(example = "아이폰 13") @NotBlank @Size(max = 100) String productName,
        @io.swagger.v3.oas.annotations.media.Schema(description = "상품 상태", example = "거의 새것") @NotBlank @Size(max = 100) String condition,
        @io.swagger.v3.oas.annotations.media.Schema(description = "선택", example = "128GB, 블루") @Size(max = 1000) String features,
        @io.swagger.v3.oas.annotations.media.Schema(description = "선택", example = "직거래 선호") @Size(max = 1000) String extraInfo
) {
}
