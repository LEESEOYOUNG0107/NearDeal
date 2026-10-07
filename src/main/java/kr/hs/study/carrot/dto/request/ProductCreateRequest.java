package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kr.hs.study.carrot.entity.Category;

public record ProductCreateRequest(
        @NotBlank String title,
        @NotNull @Positive Long price,
        @NotNull Category category,
        @NotBlank String description,
        @NotBlank String region
) {
}
