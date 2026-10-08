package kr.hs.study.carrot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.hs.study.carrot.entity.ProductStatus;

public record ProductStatusRequest(
        @Schema(description = "SALE 판매중, RESERVED 예약중, SOLD_OUT 거래완료", example = "RESERVED") @NotNull ProductStatus status) {
}
