package kr.hs.study.carrot.repository;

import kr.hs.study.carrot.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    java.util.List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    @org.springframework.data.jpa.repository.Query("select distinct p.region from Product p order by p.region")
    java.util.List<String> findDistinctRegions();
}
