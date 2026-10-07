package kr.hs.study.carrot.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// 문자열 목록을 쿠키 하나에 저장한다("값1|값2", 각 값은 URL 인코딩). 최근 검색어, 내 동네 저장에 사용
public final class CookieList {

    private static final int MAX_AGE = 60 * 60 * 24 * 180;

    private CookieList() {
    }

    public static List<String> read(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("\\|"))
                .map(t -> URLDecoder.decode(t, StandardCharsets.UTF_8))
                .filter(t -> !t.isBlank())
                .toList();
    }

    public static void write(HttpServletResponse response, String name, List<String> values) {
        String joined = values.stream()
                .map(v -> URLEncoder.encode(v, StandardCharsets.UTF_8))
                .collect(Collectors.joining("|"));
        Cookie cookie = new Cookie(name, joined);
        cookie.setPath("/");
        cookie.setMaxAge(values.isEmpty() ? 0 : MAX_AGE);
        response.addCookie(cookie);
    }
}
