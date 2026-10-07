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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// DB에 이미 있던 chat_room(id, buyer_id, product_id, seller_id) 구조에 맞춤. 상품이 삭제되면 방도 DB에서 같이 삭제(ON DELETE CASCADE)
@Entity
@Table(name = "chat_room", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "buyer_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    public ChatRoom(Product product, Long buyerId) {
        this.product = product;
        this.buyerId = buyerId;
        this.sellerId = product.getSellerId();
    }

    public boolean hasParticipant(Long userId) {
        return buyerId.equals(userId) || sellerId.equals(userId);
    }
}
