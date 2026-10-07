package kr.hs.study.carrot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

// 인증이 안 된 요청에 401과 "왜 안 됐는지"를 JSON으로 알려준다(토큰 없음 / 만료 / 위조)
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        String message = messageFor(authException);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType("application/json;charset=UTF-8");
        // message는 아래 상수 문자열 중 하나라 JSON 이스케이프가 필요 없다
        response.getOutputStream().write(("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8));
    }

    static String messageFor(AuthenticationException e) {
        if (!(e instanceof InvalidBearerTokenException)) {
            return "로그인이 필요합니다.";
        }
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains("expired")) {
                return "토큰이 만료되었습니다. 다시 로그인해주세요.";
            }
        }
        return "유효하지 않은 토큰입니다.";
    }
}
