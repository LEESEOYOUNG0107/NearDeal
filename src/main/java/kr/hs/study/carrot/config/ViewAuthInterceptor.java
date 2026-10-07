package kr.hs.study.carrot.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import kr.hs.study.carrot.controller.AuthViewController;
import kr.hs.study.carrot.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

// Thymeleaf 화면 전용 로그인 검사. 쿠키의 JWT가 유효하면 요청 속성 "uid"에 회원 번호를 넣고(컨트롤러는 @RequestAttribute("uid")로 받음),
// 아니면 로그인 화면으로 보낸다. /api/** 는 Spring Security(Authorization 헤더)가 따로 처리한다
@Component
@RequiredArgsConstructor
public class ViewAuthInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = tokenFrom(request);
        if (token != null) {
            try {
                request.setAttribute("uid", jwtProvider.parseMemberId(token));
                return true;
            } catch (ResponseStatusException e) {
                // 만료/위조된 토큰: 아래에서 로그인 화면으로 보낸다
            }
        }
        String target = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        // GET 이 아닌 요청(폼 제출)은 되돌아갈 곳이 없으므로 next 를 붙이지 않는다
        String next = "GET".equals(request.getMethod()) ? "?next=" + URLEncoder.encode(target, StandardCharsets.UTF_8) : "";
        response.sendRedirect(request.getContextPath() + "/login" + next);
        return false;
    }

    private String tokenFrom(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (AuthViewController.COOKIE.equals(c.getName()) && !c.getValue().isBlank()) return c.getValue();
        }
        return null;
    }
}
