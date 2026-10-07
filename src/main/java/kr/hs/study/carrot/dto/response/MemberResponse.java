package kr.hs.study.carrot.dto.response;

import java.time.LocalDateTime;
import kr.hs.study.carrot.entity.Member;

// 비밀번호(해시 포함)는 응답에 넣지 않는다
public record MemberResponse(Long id, String email, String nickname, String profileImage, LocalDateTime createdAt) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getNickname(),
                member.getProfileImage(), member.getCreatedAt());
    }
}
