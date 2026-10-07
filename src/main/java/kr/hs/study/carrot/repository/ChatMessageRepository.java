package kr.hs.study.carrot.repository;

import java.util.List;
import java.util.Optional;
import kr.hs.study.carrot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoomIdOrderByIdAsc(Long chatRoomId);

    Optional<ChatMessage> findFirstByChatRoomIdOrderByIdDesc(Long chatRoomId);

    long countByChatRoomIdAndSenderIdNotAndSeenFalse(Long chatRoomId, Long senderId);

    @Modifying
    @Query("update ChatMessage m set m.seen = true "
            + "where m.chatRoom.id = :roomId and m.senderId <> :userId and m.seen = false")
    int markSeen(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
