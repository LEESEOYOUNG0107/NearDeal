package kr.hs.study.carrot.repository;

import java.util.List;
import kr.hs.study.carrot.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    // [productId, filename] 쌍. 목록 카드의 대표 사진용
    @Query("select i.product.id, i.filename from ProductImage i where i.sortOrder = 0")
    List<Object[]> findCovers();
}
