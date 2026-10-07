package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// condition은 사용자가 직접 쓰는 상태 설명("거의 새것"). ProductStatus(판매중/예약중/판매완료)와 다름
public record ProductDescriptionRequest(
        @NotBlank @Size(max = 100) String productName,
        @NotBlank @Size(max = 100) String condition,
        @Size(max = 1000) String features,
        @Size(max = 1000) String extraInfo
) {
}
