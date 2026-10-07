package kr.hs.study.carrot.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class JwtAuthenticationEntryPointTest {

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

    private MockHttpServletResponse respond(AuthenticationException e) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        entryPoint.commence(new MockHttpServletRequest(), response, e);
        return response;
    }

    @Test
    void 토큰이_없으면_로그인이_필요하다고_401로_답한다() throws Exception {
        MockHttpServletResponse response = respond(new InsufficientAuthenticationException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("로그인이 필요합니다");
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    @Test
    void 만료된_토큰은_만료됐다고_알려준다() throws Exception {
        MockHttpServletResponse response = respond(
                new InvalidBearerTokenException("Jwt expired at 2026-01-01T00:00:00Z", new RuntimeException("Jwt expired at ...")));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("만료");
    }

    @Test
    void 위조되거나_깨진_토큰은_유효하지_않다고_알려준다() throws Exception {
        MockHttpServletResponse response = respond(new InvalidBearerTokenException("Signed JWT rejected: Invalid signature"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).contains("유효하지 않은 토큰");
    }
}
