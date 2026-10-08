package kr.hs.study.carrot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import java.util.List;
import kr.hs.study.carrot.dto.request.ProductCreateRequest;
import kr.hs.study.carrot.dto.request.ProductSearchCondition;
import kr.hs.study.carrot.dto.request.ProductStatusRequest;
import kr.hs.study.carrot.dto.request.ProductUpdateRequest;
import kr.hs.study.carrot.dto.response.ProductListResponse;
import kr.hs.study.carrot.dto.response.ProductResponse;
import kr.hs.study.carrot.security.CurrentUserId;
import kr.hs.study.carrot.service.NeighborhoodService;
import kr.hs.study.carrot.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. 상품", description = "상품 등록, 수정, 삭제, 목록, 검색, 상세, 내 동네 상품")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final NeighborhoodService neighborhoodService;

    @Operation(summary = "상품 등록",
            description = "판매자는 로그인한 회원으로 자동 지정됩니다. 상태는 항상 판매중으로 시작하고, 성공하면 201입니다.")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @CurrentUserId Long userId) {
        ProductResponse response = productService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "상품 수정",
            description = "보낸 필드만 바뀝니다(제목, 가격, 카테고리, 설명, 지역). 본인 상품이 아니면 403, 없으면 404입니다.")
    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(productService.updateProduct(productId, request, userId));
    }

    @Operation(summary = "거래 상태 변경",
            description = "판매중/예약중/거래완료로 바꿉니다. 본인 상품만 가능하고(403), 실제로 상태가 바뀌었을 때만 채팅한 구매자와 찜한 사람에게 알림이 갑니다. 성공하면 본문 없이 204입니다.")
    @PatchMapping("/{productId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long productId,
            @Valid @RequestBody ProductStatusRequest request,
            @CurrentUserId Long userId) {
        productService.changeStatus(productId, request.status(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상품 삭제",
            description = "본인 상품만 삭제할 수 있고(403), 성공하면 본문 없이 204입니다. 연결된 찜, 채팅방도 함께 지워집니다.")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            @CurrentUserId Long userId) {
        productService.deleteProduct(productId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상품 목록/검색",
            description = "모든 조건은 선택이고 함께 쓰면 AND로 묶입니다. keyword는 제목 일부(대소문자 무시), region은 거래 지역에 포함된 글자입니다. 최신순입니다.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<List<ProductListResponse>> getProducts(@ParameterObject ProductSearchCondition condition) {
        return ResponseEntity.ok(productService.getProducts(condition));
    }

    // 내 동네(현재 동네) 상품. 검색어/카테고리/가격/상태 조건도 같이 쓸 수 있고, 지역 조건은 내 동네로 고정된다
    @Operation(summary = "내 동네 상품",
            description = "내 동네 목록의 첫 번째(현재 동네)가 거래 지역에 포함된 상품만 돌려줍니다. 검색 조건도 같이 쓸 수 있지만 region은 무시됩니다. 동네를 설정하지 않았으면 400입니다.")
    @GetMapping("/nearby")
    public ResponseEntity<List<ProductListResponse>> getNearbyProducts(@ParameterObject ProductSearchCondition condition, @CurrentUserId Long userId) {
        String region = neighborhoodService.requireCurrent(userId);
        return ResponseEntity.ok(productService.getProducts(new ProductSearchCondition(
                condition.keyword(), condition.category(), condition.minPrice(), condition.maxPrice(), condition.status(), region)));
    }

    @Operation(summary = "상품 상세",
            description = "조회할 때마다 조회수가 1 늘어납니다. 판매자 번호와 닉네임이 함께 나오고, 없으면 404입니다.")
    @SecurityRequirements
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }
}
