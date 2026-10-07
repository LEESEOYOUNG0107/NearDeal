package kr.hs.study.carrot.repository;

import java.util.List;
import java.util.Optional;
import kr.hs.study.carrot.entity.Notification;
import kr.hs.study.carrot.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByUserIdOrderByIdDesc(Long userId);

    List<Notification> findTop50ByUserIdAndSeenFalseOrderByIdDesc(Long userId);

    long countByUserIdAndSeenFalse(Long userId);

    boolean existsByUserIdAndTypeAndProductIdAndActorId(Long userId, NotificationType type, Long productId, Long actorId);

    Optional<Notification> findFirstByUserIdAndTypeAndRoomIdAndSeenFalse(Long userId, NotificationType type, Long roomId);

    @Modifying
    @Query("update Notification n set n.seen = true where n.userId = :userId and n.seen = false")
    int markAllSeen(@Param("userId") Long userId);

    @Modifying
    @Query("update Notification n set n.seen = true "
            + "where n.userId = :userId and n.type = :type and n.roomId = :roomId and n.seen = false")
    int markRoomSeen(@Param("userId") Long userId, @Param("type") NotificationType type, @Param("roomId") Long roomId);
}
