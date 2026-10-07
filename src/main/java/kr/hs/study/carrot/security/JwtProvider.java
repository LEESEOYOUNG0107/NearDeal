package kr.hs.study.carrot.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

// JWT 발급과 검증을 한 곳에서 한다. HS256(비밀키 하나로 서명/검증), 알고리즘은 HS256으로 고정되어 "alg: none" 같은 위조 토큰은 거부된다
@Slf4j
@Component
public class JwtProvider {

    private static final String ISSUER = "neardeal";
    private static final int MIN_SECRET_BYTES = 32;

    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final Duration expiration;

    public JwtProvider(@Value("${app.jwt.secret:}") String secret,
                       @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes) {
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("app.jwt.expiration-minutes는 1 이상이어야 합니다.");
        }
        SecretKey key = new SecretKeySpec(resolveSecret(secret), "HmacSHA256");
        this.expiration = Duration.ofMinutes(expirationMinutes);
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        // 기본 검증기는 만료 후 60초까지 봐주므로, 만료 시각을 정확히 지키도록 오차 허용을 0으로 한다
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO), new JwtIssuerValidator(ISSUER)));
    }

    private byte[] resolveSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            // ponytail: 키가 없으면 시작 때마다 새로 만든다(재시작하면 기존 토큰 전부 무효). 운영에서는 JWT_SECRET 필수
            log.warn("JWT_SECRET이 설정되지 않아 임시 비밀키를 생성했습니다. 서버를 재시작하면 모든 로그인이 풀립니다.");
            byte[] random = new byte[MIN_SECRET_BYTES];
            new SecureRandom().nextBytes(random);
            return random;
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET은 " + MIN_SECRET_BYTES + "바이트(영문 32자) 이상이어야 합니다.");
        }
        return bytes;
    }

    // Spring Security의 토큰 검사 필터가 같은 검증 규칙(서명/HS256 고정/만료 오차 0/발급자)을 쓰도록 공유한다
    public JwtDecoder decoder() {
        return decoder;
    }

    public long expiresInSeconds() {
        return expiration.toSeconds();
    }

    // 토큰에는 회원 번호(sub)와 발급/만료 시각만 넣는다. payload는 누구나 읽을 수 있으므로 비밀번호 같은 민감 정보는 넣지 않는다
    public String createAccessToken(Long memberId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    // 서명 + 만료 검사를 통과하면 회원 번호를 돌려준다. 실패하면 401
    public Long parseMemberId(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            return Long.valueOf(jwt.getSubject());
        } catch (BadJwtException e) {
            boolean expired = e.getMessage() != null && e.getMessage().contains("expired");
            throw unauthorized(expired ? "토큰이 만료되었습니다. 다시 로그인해주세요." : "유효하지 않은 토큰입니다.");
        } catch (JwtException | NumberFormatException e) {
            throw unauthorized("유효하지 않은 토큰입니다.");
        }
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
