package kr.hs.study.carrot.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import kr.hs.study.carrot.entity.Member;

// 비밀번호(해시 포함)는 응답에 넣지 않는다. neighborhoods: 내 동네(첫 번째가 현재 동네)
public record MemberResponse(Long id, String email, String nickname, String profileImage, LocalDateTime createdAt,
                             List<String> neighborhoods) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getNickname(),
                member.getProfileImage(), member.getCreatedAt(), member.neighborhoodList());
    }
}
