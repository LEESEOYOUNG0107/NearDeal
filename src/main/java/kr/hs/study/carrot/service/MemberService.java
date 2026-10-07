package kr.hs.study.carrot.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import kr.hs.study.carrot.dto.request.LoginRequest;
import kr.hs.study.carrot.dto.request.SignupRequest;
import kr.hs.study.carrot.dto.response.LoginResponse;
import kr.hs.study.carrot.dto.response.MemberResponse;
import kr.hs.study.carrot.entity.Member;
import kr.hs.study.carrot.repository.MemberRepository;
import kr.hs.study.carrot.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 64;
    private static final int BCRYPT_MAX_BYTES = 72;

    // 없는 이메일로 로그인할 때도 비밀번호 비교를 한 번 해서, 응답 시간으로 가입 여부를 알아내지 못하게 한다
    private static final String DUMMY_HASH = new BCryptPasswordEncoder().encode("dummy-password-for-timing");

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // ponytail: 로그인 시도 횟수 제한이 없음(무차별 대입 가능). 운영 전 IP/계정별 제한 필요
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<Member> found = memberRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT));
        String hash = found.map(Member::getPassword).orElse(DUMMY_HASH);
        // 72바이트를 넘는 비밀번호는 가입할 수 없으므로 비교하지 않고 실패 처리(BCrypt가 예외를 던질 수 있음)
        boolean matches = request.password().getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES
                && passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        Member member = found.get();
        return new LoginResponse(jwtProvider.createAccessToken(member.getId()), "Bearer",
                jwtProvider.expiresInSeconds(), MemberResponse.from(member));
    }

    // 토큰 검증은 Spring Security 필터가 끝낸 뒤이므로, 여기서는 토큰의 회원이 아직 존재하는지만 확인한다
    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 회원입니다."));
    }

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        validatePassword(request.password());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByEmail(email)) {
            throw emailTaken();
        }
        String profileImage = StringUtils.hasText(request.profileImage()) ? request.profileImage() : null;
        Member member = new Member(email, passwordEncoder.encode(request.password()), request.nickname().trim(), profileImage);
        try {
            return MemberResponse.from(memberRepository.save(member));
        } catch (DataIntegrityViolationException e) {
            // 같은 이메일이 동시에 가입된 경우(유니크 제약)
            throw emailTaken();
        }
    }

    // BCrypt는 72바이트를 넘는 부분을 무시하거나 거부하므로 한글(3바이트)까지 고려해 바이트로도 제한한다
    private void validatePassword(String password) {
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX
                || password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "비밀번호는 " + PASSWORD_MIN + "~" + PASSWORD_MAX + "자여야 합니다.");
        }
    }

    private ResponseStatusException emailTaken() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
    }
}
