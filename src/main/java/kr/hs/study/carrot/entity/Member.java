package kr.hs.study.carrot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

// 이 id가 상품/찜/채팅에서 쓰는 userId가 된다(연결은 18단계). user는 Oracle 예약어라 member로 이름 지음
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // BCrypt 해시(60자)만 저장한다. 원문 비밀번호는 어디에도 저장하지 않는다
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(length = 500)
    private String profileImage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Member(String email, String encodedPassword, String nickname, String profileImage) {
        this.email = email;
        this.password = encodedPassword;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }
}
