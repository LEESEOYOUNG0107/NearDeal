package kr.hs.study.carrot.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import kr.hs.study.carrot.entity.Member;
import kr.hs.study.carrot.repository.MemberRepository;
import kr.hs.study.carrot.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 회원의 동네(최대 2개, 첫 번째가 현재 동네)를 관리한다. 상품과의 연결은 "상품 거래 지역에 동네 이름이 포함되는가"
@Service
@RequiredArgsConstructor
public class NeighborhoodService {

    public static final int MAX = 2;
    private static final Pattern VALID = Pattern.compile("^[0-9A-Za-z가-힣 ]{1,20}$");
    // ponytail: 동네 검색용 기본 목록(샘플). 행정구역 데이터를 가져오면 교체. 상품에 쓰인 지역은 자동으로 합쳐진다
    private static final List<String> SAMPLE = List.of("성수동", "연남동", "망원동", "합정동", "이태원동", "한남동",
            "서교동", "역삼동", "삼성동", "잠실동", "신림동", "상암동");

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    @Transactional(readOnly = true)
    public List<String> get(Long memberId) {
        return findMember(memberId).neighborhoodList();
    }

    // 현재 동네(첫 번째). 설정하지 않았으면 null = 전체 동네
    @Transactional(readOnly = true)
    public String current(Long memberId) {
        List<String> mine = get(memberId);
        return mine.isEmpty() ? null : mine.get(0);
    }

    @Transactional(readOnly = true)
    public String requireCurrent(Long memberId) {
        String current = current(memberId);
        if (current == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내 동네를 먼저 설정해 주세요.");
        }
        return current;
    }

    // 목록 전체를 한 번에 바꾼다(추가/삭제/현재 동네 변경 모두 이 하나로 처리). 빈 목록이면 동네 설정을 해제한다
    @Transactional
    public List<String> set(Long memberId, List<String> names) {
        List<String> cleaned = names.stream().map(n -> n == null ? "" : n.strip()).toList();
        if (cleaned.size() > MAX) {
            throw bad("동네는 최대 " + MAX + "개까지 설정할 수 있어요.");
        }
        if (cleaned.stream().anyMatch(n -> !isValid(n))) {
            throw bad("동네 이름은 한글, 영문, 숫자, 공백으로 1~20자여야 해요.");
        }
        if (cleaned.stream().map(n -> n.toLowerCase(Locale.ROOT)).distinct().count() != cleaned.size()) {
            throw bad("같은 동네를 두 번 설정할 수 없어요.");
        }
        findMember(memberId).changeNeighborhoods(cleaned);
        return cleaned;
    }

    // 샘플 + 상품에 실제로 쓰인 지역 중에서 키워드가 들어간 이름. 동네로 설정할 수 있는(규칙에 맞는) 이름만 돌려준다
    @Transactional(readOnly = true)
    public List<String> search(String keyword, int limit) {
        String q = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        Set<String> all = new LinkedHashSet<>(SAMPLE);
        all.addAll(productRepository.findDistinctRegions());
        return all.stream()
                .filter(NeighborhoodService::isValid)
                .filter(r -> q.isEmpty() || r.toLowerCase(Locale.ROOT).contains(q))
                .limit(limit)
                .toList();
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 회원입니다."));
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
