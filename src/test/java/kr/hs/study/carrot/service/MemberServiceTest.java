package kr.hs.study.carrot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.hs.study.carrot.dto.request.LoginRequest;
import kr.hs.study.carrot.dto.request.SignupRequest;
import kr.hs.study.carrot.entity.Member;
import kr.hs.study.carrot.repository.MemberRepository;
import kr.hs.study.carrot.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock MemberRepository memberRepository;
    @Spy PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Mock JwtProvider jwtProvider;
    @InjectMocks MemberService memberService;

    private Member memberWithPassword(String rawPassword) {
        return new Member("a@a.com", passwordEncoder.encode(rawPassword), "당근이", null);
    }

    @Test
    void 이메일과_비밀번호가_맞으면_토큰을_발급한다() {
        Member member = memberWithPassword("my-secret-pw");
        when(memberRepository.findByEmail("a@a.com")).thenReturn(Optional.of(member));
        when(jwtProvider.createAccessToken(any())).thenReturn("the-token");

        var response = memberService.login(new LoginRequest(" A@a.com ", "my-secret-pw"));

        assertThat(response.accessToken()).isEqualTo("the-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.member().email()).isEqualTo("a@a.com");
    }

    @Test
    void 비밀번호가_틀린_경우와_없는_이메일은_똑같은_401_메시지로_답한다() {
        Member member = memberWithPassword("my-secret-pw");
        when(memberRepository.findByEmail("a@a.com")).thenReturn(Optional.of(member));
        when(memberRepository.findByEmail("none@a.com")).thenReturn(Optional.empty());

        var wrongPassword = catchThrowable(() -> memberService.login(new LoginRequest("a@a.com", "wrong-password")));
        var unknownEmail = catchThrowable(() -> memberService.login(new LoginRequest("none@a.com", "my-secret-pw")));

        assertStatus(wrongPassword, HttpStatus.UNAUTHORIZED);
        assertStatus(unknownEmail, HttpStatus.UNAUTHORIZED);
        assertThat(((ResponseStatusException) wrongPassword).getReason())
                .isEqualTo(((ResponseStatusException) unknownEmail).getReason());
        verify(jwtProvider, never()).createAccessToken(any());
    }

    @Test
    void 비밀번호가_72바이트를_넘어도_500이_아니라_401이다() {
        Member member = memberWithPassword("my-secret-pw");
        when(memberRepository.findByEmail("a@a.com")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.login(new LoginRequest("a@a.com", "x".repeat(200))))
                .satisfies(e -> assertStatus(e, HttpStatus.UNAUTHORIZED));
    }

    @Test
    void 토큰의_회원이_존재하지_않으면_401이다() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(99L)).satisfies(e -> assertStatus(e, HttpStatus.UNAUTHORIZED));
    }

    private SignupRequest request(String email, String password) {
        return new SignupRequest(email, password, "당근이", null);
    }

    private void assertStatus(Throwable e, HttpStatus expected) {
        assertThat(e).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(expected));
    }

    @Test
    void 비밀번호는_원문이_아니라_BCrypt_해시로_저장되고_이메일은_정규화된다() {
        when(memberRepository.save(any(Member.class))).thenAnswer(i -> i.getArgument(0));

        memberService.signup(request("  Hello@Example.COM ", "my-secret-pw"));

        ArgumentCaptor<Member> saved = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("hello@example.com");
        assertThat(saved.getValue().getPassword()).isNotEqualTo("my-secret-pw").startsWith("$2");
        assertThat(passwordEncoder.matches("my-secret-pw", saved.getValue().getPassword())).isTrue();
    }

    @Test
    void 이미_가입된_이메일은_409이고_저장하지_않는다() {
        when(memberRepository.existsByEmail("a@a.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.signup(request("A@a.com", "my-secret-pw")))
                .satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void 동시_가입으로_유니크_제약에_걸려도_409로_돌려준다() {
        when(memberRepository.save(any(Member.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> memberService.signup(request("a@a.com", "my-secret-pw")))
                .satisfies(e -> assertStatus(e, HttpStatus.CONFLICT));
    }

    @Test
    void 너무_짧거나_긴_비밀번호는_400이다_한글은_바이트로_계산한다() {
        assertThatThrownBy(() -> memberService.signup(request("a@a.com", "short")))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> memberService.signup(request("a@a.com", "a".repeat(65))))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        // 25자 * 3바이트 = 75바이트 > 72
        assertThatThrownBy(() -> memberService.signup(request("a@a.com", "가".repeat(25))))
                .satisfies(e -> assertStatus(e, HttpStatus.BAD_REQUEST));
        verify(memberRepository, never()).save(any());
    }
}
