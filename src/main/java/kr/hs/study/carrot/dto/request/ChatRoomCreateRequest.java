package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChatRoomCreateRequest(@NotNull Long productId) {
}
