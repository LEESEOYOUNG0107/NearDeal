package kr.hs.study.carrot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.hs.study.carrot.dto.response.FavoriteStatusResponse;
import kr.hs.study.carrot.dto.response.ProductListResponse;
import kr.hs.study.carrot.security.CurrentUserId;
import kr.hs.study.carrot.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "3. 찜", description = "상품 찜하기와 찜 목록")
@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "찜하기",
            description = "이미 찜했으면 409입니다. 판매자에게 찜 알림이 가고, 자기 상품을 찜해도 알림은 가지 않습니다.")
    @PostMapping("/api/products/{productId}/favorite")
    public ResponseEntity<FavoriteStatusResponse> add(
            @PathVariable Long productId,
            @CurrentUserId Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(favoriteService.add(productId, userId));
    }

    @Operation(summary = "찜 취소")
    @DeleteMapping("/api/products/{productId}/favorite")
    public ResponseEntity<FavoriteStatusResponse> remove(
            @PathVariable Long productId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(favoriteService.remove(productId, userId));
    }

    @Operation(summary = "찜 상태 확인",
            description = "전체 찜 개수와 내가 찜했는지를 돌려줍니다.")
    @GetMapping("/api/products/{productId}/favorite")
    public ResponseEntity<FavoriteStatusResponse> getStatus(
            @PathVariable Long productId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(favoriteService.getStatus(productId, userId));
    }

    @Operation(summary = "내 찜 목록")
    @GetMapping("/api/favorites")
    public ResponseEntity<List<ProductListResponse>> getMyFavorites(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(favoriteService.getMyFavorites(userId));
    }
}
