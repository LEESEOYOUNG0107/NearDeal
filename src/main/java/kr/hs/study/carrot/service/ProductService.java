package kr.hs.study.carrot.service;

import java.util.List;
import kr.hs.study.carrot.dto.request.ProductCreateRequest;
import kr.hs.study.carrot.dto.request.ProductSearchCondition;
import kr.hs.study.carrot.dto.request.ProductUpdateRequest;
import kr.hs.study.carrot.dto.response.ProductListResponse;
import kr.hs.study.carrot.dto.response.ProductResponse;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.entity.ProductStatus;
import kr.hs.study.carrot.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public List<ProductListResponse> getProducts(ProductSearchCondition condition) {
        return productRepository.findAll(toSpecification(condition), Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(ProductListResponse::from)
                .toList();
    }

    private Specification<Product> toSpecification(ProductSearchCondition c) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (StringUtils.hasText(c.keyword())) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), "%" + c.keyword().toLowerCase() + "%"));
        }
        if (c.category() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), c.category()));
        }
        if (c.minPrice() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.<Long>get("price"), c.minPrice()));
        }
        if (c.maxPrice() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.<Long>get("price"), c.maxPrice()));
        }
        if (c.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), c.status()));
        }
        if (StringUtils.hasText(c.region())) {
            spec = spec.and((root, query, cb) -> cb.like(root.get("region"), "%" + c.region() + "%"));
        }
        return spec;
    }

    // ponytail: 읽고-더하기 방식이라 동시 조회가 많으면 일부 누락됨. 필요해지면 UPDATE ... SET view_count = view_count + 1 쿼리로 교체
    @Transactional
    public ProductResponse getProduct(Long productId) {
        Product product = findProduct(productId);
        product.increaseViewCount();
        return ProductResponse.from(product);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다. id=" + productId));
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, Long userId) {
        Product product = findOwnedProduct(productId, userId);
        product.update(request.title(), request.price(), request.category(), request.description(), request.region());
        return ProductResponse.from(product);
    }

    @Transactional
    public void changeStatus(Long productId, ProductStatus status, Long userId) {
        Product product = findOwnedProduct(productId, userId);
        if (product.getStatus() == status) {
            return;
        }
        product.changeStatus(status);
        notificationService.onStatusChanged(product, userId);
    }

    public List<ProductListResponse> getMyProducts(Long sellerId) {
        return productRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .map(ProductListResponse::from)
                .toList();
    }

    @Transactional
    public void deleteProduct(Long productId, Long userId) {
        productRepository.delete(findOwnedProduct(productId, userId));
    }

    private Product findOwnedProduct(Long productId, Long userId) {
        Product product = findProduct(productId);
        if (!product.getSellerId().equals(userId)) {
              throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 등록한 상품만 수정/삭제할 수 있습니다.");
        }
        return product;
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, Long sellerId) {
        Product product = Product.builder()
                .sellerId(sellerId)
                .title(request.title())
                .price(request.price())
                .category(request.category())
                .description(request.description())
                .region(request.region())
                .build();

        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }
}
