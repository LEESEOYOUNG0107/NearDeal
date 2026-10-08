package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kr.hs.study.carrot.entity.Category;

// null이면 "수정 안 함", 값이 있으면 공백만 있는 문자열은 거부
public record ProductUpdateRequest(
        @Pattern(regexp = "(?s).*\\S.*") @Size(max = 100) String title,
        @io.swagger.v3.oas.annotations.media.Schema(example = "450000") @Positive @Max(9_999_999_999L) Long price,
        Category category,
        @Pattern(regexp = "(?s).*\\S.*") @Size(max = 3000) String description,
        @Pattern(regexp = "(?s).*\\S.*") @Size(max = 50) String region
) {
}
