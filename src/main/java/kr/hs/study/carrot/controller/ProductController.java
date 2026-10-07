package kr.hs.study.carrot.controller;

import jakarta.validation.Valid;
import java.util.List;
import kr.hs.study.carrot.dto.request.ProductCreateRequest;
import kr.hs.study.carrot.dto.request.ProductSearchCondition;
import kr.hs.study.carrot.dto.request.ProductUpdateRequest;
import kr.hs.study.carrot.dto.response.ProductListResponse;
import kr.hs.study.carrot.dto.response.ProductResponse;
import kr.hs.study.carrot.security.CurrentUserId;
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

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @CurrentUserId Long userId) {
        ProductResponse response = productService.createProduct(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @CurrentUserId Long userId) {
        return ResponseEntity.ok(productService.updateProduct(productId, request, userId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            @CurrentUserId Long userId) {
        productService.deleteProduct(productId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ProductListResponse>> getProducts(ProductSearchCondition condition) {
        return ResponseEntity.ok(productService.getProducts(condition));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }
}
