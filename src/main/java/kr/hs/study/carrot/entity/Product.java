package kr.hs.study.carrot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sellerId;

    private String title;

    private Long price;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private String region;

    private int viewCount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder
    private Product(Long sellerId, String title, Long price, Category category, String description, String region) {
        this.sellerId = sellerId;
        this.title = title;
        this.price = price;
        this.category = category;
        this.description = description;
        this.region = region;
        this.status = ProductStatus.SALE;
        this.viewCount = 0;
    }

    public void update(String title, Long price, Category category, String description, String region) {
        if (title != null) this.title = title;
        if (price != null) this.price = price;
        if (category != null) this.category = category;
        if (description != null) this.description = description;
        if (region != null) this.region = region;
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }
}
