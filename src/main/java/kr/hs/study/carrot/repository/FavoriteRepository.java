package kr.hs.study.carrot.repository;

import java.util.List;
import java.util.Optional;
import kr.hs.study.carrot.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    long countByProductId(Long productId);

    @Query("select f from Favorite f join fetch f.product where f.userId = :userId order by f.id desc")
    List<Favorite> findAllByUser(@Param("userId") Long userId);

    @Query("select f.product.id from Favorite f where f.userId = :userId")
    List<Long> findProductIdsByUser(@Param("userId") Long userId);

    // [productId, count] 쌍. 목록 화면의 찜 수 표시용
    @Query("select f.product.id, count(f) from Favorite f group by f.product.id")
    List<Object[]> countGroupedByProduct();

    @Query("select f.userId from Favorite f where f.product.id = :productId")
    List<Long> findUserIdsByProductId(@Param("productId") Long productId);
}
