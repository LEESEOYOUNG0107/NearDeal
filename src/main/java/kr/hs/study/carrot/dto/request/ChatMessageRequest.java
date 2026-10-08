package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(@io.swagger.v3.oas.annotations.media.Schema(example = "안녕하세요, 아직 판매중인가요?") @NotBlank @Size(max = 1000) String content) {
}
