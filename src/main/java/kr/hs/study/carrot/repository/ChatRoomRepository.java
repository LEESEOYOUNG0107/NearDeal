package kr.hs.study.carrot.repository;

import java.util.List;
import java.util.Optional;
import kr.hs.study.carrot.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByProductIdAndBuyerId(Long productId, Long buyerId);

    List<ChatRoom> findByProductId(Long productId);

    @Query("select r from ChatRoom r join fetch r.product "
            + "where r.buyerId = :userId or r.sellerId = :userId order by r.id desc")
    List<ChatRoom> findAllByParticipant(@Param("userId") Long userId);
}
