package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import kr.hs.study.carrot.entity.Category;

// null이면 "수정 안 함", 값이 있으면 공백만 있는 문자열은 거부
public record ProductUpdateRequest(
        @Pattern(regexp = "(?s).*\\S.*") String title,
        @Positive Long price,
        Category category,
        @Pattern(regexp = "(?s).*\\S.*") String description,
        @Pattern(regexp = "(?s).*\\S.*") String region
) {
}
