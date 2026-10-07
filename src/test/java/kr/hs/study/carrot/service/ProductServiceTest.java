package kr.hs.study.carrot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.hs.study.carrot.dto.request.ProductCreateRequest;
import kr.hs.study.carrot.dto.request.ProductUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import kr.hs.study.carrot.dto.response.ProductResponse;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.entity.ProductStatus;
import kr.hs.study.carrot.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProductService productService;

    @Test
    void 상태가_실제로_바뀔_때만_알림_처리를_호출한다() {
        Product product = Product.builder().sellerId(1L).title("아이폰").price(1000L)
                .category(Category.DIGITAL).description("설명").region("서울").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.changeStatus(1L, ProductStatus.SALE, 1L);
        verify(notificationService, never()).onStatusChanged(any(), any());

        productService.changeStatus(1L, ProductStatus.RESERVED, 1L);
        verify(notificationService).onStatusChanged(product, 1L);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.RESERVED);
    }

    @Test
    void 상품을_등록하면_판매중_상태로_저장된다() {
        ProductCreateRequest request = new ProductCreateRequest(
                "아이폰 13", 500000L, Category.DIGITAL, "거의 새것입니다", "서울시 강남구");

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.createProduct(request, 1L);

        assertThat(response.title()).isEqualTo("아이폰 13");
        assertThat(response.sellerId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(ProductStatus.SALE);
    }

    private Product productOf(Long sellerId) {
        return Product.builder().sellerId(sellerId).title("아이폰 13").price(500000L)
                .category(Category.DIGITAL).description("설명").region("서울").build();
    }

    @Test
    void 없는_상품을_조회하면_404() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void 다른_사용자는_상품을_수정_삭제할_수_없다() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(productOf(1L)));
        ProductUpdateRequest request = new ProductUpdateRequest("해킹", null, null, null, null);

        assertThatThrownBy(() -> productService.updateProduct(1L, request, 2L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> productService.deleteProduct(1L, 2L))
                .isInstanceOf(ResponseStatusException.class);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void 판매자는_보낸_필드만_수정된다() {
        Product product = productOf(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.updateProduct(
                1L, new ProductUpdateRequest("아이폰 14", null, null, null, null), 1L);

        assertThat(response.title()).isEqualTo("아이폰 14");
        assertThat(response.price()).isEqualTo(500000L);
    }
}
