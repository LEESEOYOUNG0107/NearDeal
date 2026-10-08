package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatRoomCreateRequest(@io.swagger.v3.oas.annotations.media.Schema(description = "문의할 상품 번호", example = "1") @NotNull Long productId) {
}
