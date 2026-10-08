package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

// 내 동네 전체 목록. 첫 번째가 현재 동네, 최대 2개, 빈 목록이면 설정 해제. 규칙 검사는 NeighborhoodService가 한다
public record NeighborhoodUpdateRequest(@io.swagger.v3.oas.annotations.media.Schema(description = "최대 2개, 첫 번째가 현재 동네", example = "[\"성수동\", \"역삼동\"]") @NotNull List<String> neighborhoods) {
}
