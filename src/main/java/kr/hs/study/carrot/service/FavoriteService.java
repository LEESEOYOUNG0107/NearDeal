package kr.hs.study.carrot.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.hs.study.carrot.dto.response.FavoriteStatusResponse;
import kr.hs.study.carrot.dto.response.ProductListResponse;
import kr.hs.study.carrot.entity.Favorite;
import kr.hs.study.carrot.entity.Product;
import kr.hs.study.carrot.repository.FavoriteRepository;
import kr.hs.study.carrot.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    // ponytail: 같은 사용자가 동시에 두 번 누르면 유니크 제약 때문에 한쪽이 500. 중복 저장은 DB가 막으므로 데이터는 안전
    @Transactional
    public FavoriteStatusResponse add(Long productId, Long userId) {
        Product product = findProduct(productId);
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 찜한 상품입니다.");
        }
        favoriteRepository.save(new Favorite(userId, product));
        notificationService.onFavorited(product, userId);
        return status(productId, userId);
    }

    @Transactional
    public FavoriteStatusResponse remove(Long productId, Long userId) {
        Favorite favorite = favoriteRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "찜하지 않은 상품입니다."));
        favoriteRepository.delete(favorite);
        return status(productId, userId);
    }

    @Transactional(readOnly = true)
    public FavoriteStatusResponse getStatus(Long productId, Long userId) {
        findProduct(productId);
        return status(productId, userId);
    }

    @Transactional(readOnly = true)
    public List<ProductListResponse> getMyFavorites(Long userId) {
        return favoriteRepository.findAllByUser(userId).stream()
                .map(f -> ProductListResponse.from(f.getProduct()))
                .toList();
    }

    @Transactional
    public FavoriteStatusResponse toggle(Long productId, Long userId) {
        return favoriteRepository.existsByUserIdAndProductId(userId, productId)
                ? remove(productId, userId)
                : add(productId, userId);
    }

    @Transactional(readOnly = true)
    public Set<Long> favoriteProductIds(Long userId) {
        return new HashSet<>(favoriteRepository.findProductIdsByUser(userId));
    }

    // ponytail: 전체 상품의 찜 수를 한 번에 집계. 상품이 수만 개로 늘면 화면에 보이는 id만 조회하도록 교체
    @Transactional(readOnly = true)
    public Map<Long, Long> favoriteCounts() {
        Map<Long, Long> counts = new HashMap<>();
        favoriteRepository.countGroupedByProduct().forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        return counts;
    }

    private FavoriteStatusResponse status(Long productId, Long userId) {
        return new FavoriteStatusResponse(
                favoriteRepository.countByProductId(productId),
                favoriteRepository.existsByUserIdAndProductId(userId, productId));
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다. id=" + productId));
    }
}
