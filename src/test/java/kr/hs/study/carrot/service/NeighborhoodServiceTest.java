package kr.hs.study.carrot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kr.hs.study.carrot.entity.Member;
import kr.hs.study.carrot.repository.MemberRepository;
import kr.hs.study.carrot.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NeighborhoodServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock ProductRepository productRepository;
    @InjectMocks NeighborhoodService service;

    private Member member() {
        Member m = new Member("a@a.com", "hash", "당근이", null);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(m));
        return m;
    }

    private void assertBadRequest(Throwable e) {
        assertThat(e).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void 동네를_설정하면_다듬어서_저장되고_첫_번째가_현재_동네다() {
        Member m = member();

        List<String> saved = service.set(1L, List.of("  성수동 ", "역삼동"));

        assertThat(saved).containsExactly("성수동", "역삼동");
        assertThat(m.neighborhoodList()).containsExactly("성수동", "역삼동");
        assertThat(service.current(1L)).isEqualTo("성수동");
    }

    @Test
    void 빈_목록으로_설정하면_동네_설정이_해제된다() {
        Member m = member();
        service.set(1L, List.of("성수동"));

        service.set(1L, List.of());

        assertThat(m.neighborhoodList()).isEmpty();
        assertThat(service.current(1L)).isNull();
    }

    @Test
    void 세_개_이상_중복_이상한_이름은_회원을_조회하기_전에_400으로_거부한다() {
        assertThatThrownBy(() -> service.set(1L, List.of("성수동", "역삼동", "삼성동"))).satisfies(this::assertBadRequest);
        assertThatThrownBy(() -> service.set(1L, List.of("성수동", "성수동"))).satisfies(this::assertBadRequest);
        assertThatThrownBy(() -> service.set(1L, List.of("a,b"))).satisfies(this::assertBadRequest);
        assertThatThrownBy(() -> service.set(1L, List.of("<script>"))).satisfies(this::assertBadRequest);
        assertThatThrownBy(() -> service.set(1L, List.of("가".repeat(21)))).satisfies(this::assertBadRequest);
        assertThatThrownBy(() -> service.set(1L, List.of("  "))).satisfies(this::assertBadRequest);
        assertThatThrownBy(() -> service.set(1L, java.util.Arrays.asList("성수동", null))).satisfies(this::assertBadRequest);
    }

    @Test
    void 동네를_설정하지_않았으면_내_동네_상품_조회는_400이다() {
        member();

        assertThatThrownBy(() -> service.requireCurrent(1L)).satisfies(this::assertBadRequest);
    }

    @Test
    void 동네_검색은_샘플과_상품에_쓰인_지역을_합치고_설정할_수_없는_이름은_뺀다() {
        when(productRepository.findDistinctRegions())
                .thenReturn(List.of("성수동", "서울시 성수1가", "성수 카페거리(2번출구)", "부산시 해운대구"));

        assertThat(service.search("성수", 20)).containsExactly("성수동", "서울시 성수1가");
        assertThat(service.search("해운대", 20)).containsExactly("부산시 해운대구");
        assertThat(service.search("", 3)).hasSize(3);
    }
}
