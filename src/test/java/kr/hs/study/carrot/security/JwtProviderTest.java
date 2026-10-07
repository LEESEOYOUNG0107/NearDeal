package kr.hs.study.carrot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.server.ResponseStatusException;

class JwtProviderTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-1234";

    private void assertUnauthorized(Throwable e, String messagePart) {
        assertThat(e).isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(ex.getReason()).contains(messagePart);
        });
    }

    private String b64(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void 발급한_토큰에서_회원_번호를_꺼낼_수_있다() {
        JwtProvider provider = new JwtProvider(SECRET, 60);

        String token = provider.createAccessToken(42L);

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(provider.parseMemberId(token)).isEqualTo(42L);
        assertThat(provider.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void 만료된_토큰은_만료됐다고_알려주며_401이다() {
        JwtProvider provider = new JwtProvider(SECRET, 60);
        // 같은 비밀키로 "2시간 전 발급, 1시간 전 만료"인 토큰을 직접 만든다
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant now = Instant.now();
        String expiredToken = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("neardeal").subject("1")
                        .issuedAt(now.minus(2, ChronoUnit.HOURS)).expiresAt(now.minus(1, ChronoUnit.HOURS)).build()))
                .getTokenValue();

        assertThatThrownBy(() -> provider.parseMemberId(expiredToken)).satisfies(e -> assertUnauthorized(e, "만료"));
    }

    @Test
    void 만료_직후_1초만_지나도_거부한다() {
        JwtProvider provider = new JwtProvider(SECRET, 60);
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        Instant now = Instant.now();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("neardeal").subject("1")
                        .issuedAt(now.minusSeconds(10)).expiresAt(now.minusSeconds(1)).build()))
                .getTokenValue();

        assertThatThrownBy(() -> provider.parseMemberId(token)).satisfies(e -> assertUnauthorized(e, "만료"));
    }

    @Test
    void 다른_비밀키로_서명했거나_내용을_바꾼_토큰은_거부한다() {
        JwtProvider provider = new JwtProvider(SECRET, 60);
        JwtProvider attacker = new JwtProvider("attacker-secret-attacker-secret-12345", 60);

        String forged = attacker.createAccessToken(1L);
        assertThatThrownBy(() -> provider.parseMemberId(forged)).satisfies(e -> assertUnauthorized(e, "유효하지 않은"));

        // payload만 다른 회원 번호로 바꿔치기 (서명은 그대로)
        String[] parts = provider.createAccessToken(1L).split("\\.");
        String tampered = parts[0] + "." + b64("{\"iss\":\"neardeal\",\"sub\":\"999\",\"exp\":9999999999}") + "." + parts[2];
        assertThatThrownBy(() -> provider.parseMemberId(tampered)).satisfies(e -> assertUnauthorized(e, "유효하지 않은"));
    }

    @Test
    void 서명이_없는_alg_none_토큰과_엉터리_문자열은_거부한다() {
        JwtProvider provider = new JwtProvider(SECRET, 60);
        String none = b64("{\"alg\":\"none\"}") + "." + b64("{\"iss\":\"neardeal\",\"sub\":\"1\",\"exp\":9999999999}") + ".";

        assertThatThrownBy(() -> provider.parseMemberId(none)).satisfies(e -> assertUnauthorized(e, "유효하지 않은"));
        assertThatThrownBy(() -> provider.parseMemberId("garbage")).satisfies(e -> assertUnauthorized(e, "유효하지 않은"));
    }

    @Test
    void 너무_짧은_비밀키나_잘못된_만료_시간으로는_시작할_수_없다() {
        assertThatThrownBy(() -> new JwtProvider("too-short", 60)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProvider(SECRET, 0)).isInstanceOf(IllegalStateException.class);
    }
}
