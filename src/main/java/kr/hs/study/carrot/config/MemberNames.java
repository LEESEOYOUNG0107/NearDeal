package kr.hs.study.carrot.config;

import kr.hs.study.carrot.entity.Member;
import kr.hs.study.carrot.repository.MemberRepository;
import kr.hs.study.carrot.service.ChatService;
import org.springframework.stereotype.Component;

// 표시 이름을 회원 닉네임으로. ponytail: 호출마다 회원을 조회한다. 목록이 길어지면 캐시나 fetch join 으로 교체
@Component
public class MemberNames {

    public MemberNames(MemberRepository memberRepository) {
        ChatService.useNameResolver(id -> memberRepository.findById(id).map(Member::getNickname).orElse("탈퇴한 사용자"));
    }
}
