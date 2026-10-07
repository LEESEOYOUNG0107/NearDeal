package kr.hs.study.carrot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// 상품 사진. 파일은 디스크(app.upload-dir)에 저장하고 DB에는 파일명만 둔다. sortOrder 0 이 대표 사진.
// 상품이 삭제되면 행은 DB가 같이 지운다(파일은 남는다)
@Entity
@Table(name = "product_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @Column(nullable = false, length = 100)
    private String filename;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public ProductImage(Product product, String filename, int sortOrder) {
        this.product = product;
        this.filename = filename;
        this.sortOrder = sortOrder;
    }
}
