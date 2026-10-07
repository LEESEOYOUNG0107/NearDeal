package kr.hs.study.carrot.controller;

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

@RestController
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/api/products/{productId}/favorite")
    public ResponseEntity<FavoriteStatusResponse> add(
            @PathVariable Long productId,
            @CurrentUserId Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(favoriteService.add(productId, userId));
    }

    @DeleteMapping("/api/products/{productId}/favorite")
    public ResponseEntity<FavoriteStatusResponse> remove(
            @PathVariable Long productId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(favoriteService.remove(productId, userId));
    }

    @GetMapping("/api/products/{productId}/favorite")
    public ResponseEntity<FavoriteStatusResponse> getStatus(
            @PathVariable Long productId,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(favoriteService.getStatus(productId, userId));
    }

    @GetMapping("/api/favorites")
    public ResponseEntity<List<ProductListResponse>> getMyFavorites(
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(favoriteService.getMyFavorites(userId));
    }
}
