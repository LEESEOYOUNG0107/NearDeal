package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kr.hs.study.carrot.entity.Category;

public record ProductCreateRequest(
        @io.swagger.v3.oas.annotations.media.Schema(example = "아이폰 13 팝니다") @NotBlank @Size(max = 100) String title,
        @io.swagger.v3.oas.annotations.media.Schema(description = "원 단위", example = "500000") @NotNull @Positive @Max(9_999_999_999L) Long price,
        @NotNull Category category,
        @io.swagger.v3.oas.annotations.media.Schema(example = "배터리 효율 90%, 생활기스 조금 있어요") @NotBlank @Size(max = 3000) String description,
        @io.swagger.v3.oas.annotations.media.Schema(example = "서울 성수동") @NotBlank @Size(max = 50) String region
) {
}
