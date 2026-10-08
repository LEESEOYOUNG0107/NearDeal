package kr.hs.study.carrot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import kr.hs.study.carrot.dto.request.NeighborhoodUpdateRequest;
import kr.hs.study.carrot.security.CurrentUserId;
import kr.hs.study.carrot.service.NeighborhoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6. 동네", description = "내 동네 설정(최대 2개, 첫 번째가 현재 동네)과 동네 검색")
@RestController
@RequiredArgsConstructor
public class NeighborhoodController {

    private static final int SEARCH_LIMIT = 20;

    private final NeighborhoodService neighborhoodService;

    // 동네 검색: 키워드가 들어간 동네 이름(비우면 전체 중 앞쪽 20개)
    @Operation(summary = "동네 검색",
            description = "키워드가 들어간 동네 이름을 최대 20개 돌려줍니다. 이미 등록된 상품의 지역과 기본 동네 목록에서 찾습니다.")
    @GetMapping("/api/regions")
    public ResponseEntity<List<String>> search(@RequestParam(defaultValue = "") String keyword) {
        return ResponseEntity.ok(neighborhoodService.search(keyword, SEARCH_LIMIT));
    }

    @Operation(summary = "내 동네 조회")
    @GetMapping("/api/members/me/neighborhoods")
    public ResponseEntity<List<String>> myNeighborhoods(@CurrentUserId Long userId) {
        return ResponseEntity.ok(neighborhoodService.get(userId));
    }

    @Operation(summary = "내 동네 설정",
            description = "목록 전체를 바꿉니다. 최대 2개, 한글/영문/숫자/공백 1~20자, 중복 불가이며 어기면 400입니다. 빈 목록이면 동네 설정이 해제됩니다.")
    @PutMapping("/api/members/me/neighborhoods")
    public ResponseEntity<List<String>> setMyNeighborhoods(
            @Valid @RequestBody NeighborhoodUpdateRequest request,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(neighborhoodService.set(userId, request.neighborhoods()));
    }
}
