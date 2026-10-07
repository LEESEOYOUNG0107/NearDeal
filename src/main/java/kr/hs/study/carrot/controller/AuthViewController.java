package kr.hs.study.carrot.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.regex.Pattern;
import kr.hs.study.carrot.dto.request.LoginRequest;
import kr.hs.study.carrot.dto.request.SignupRequest;
import kr.hs.study.carrot.dto.response.LoginResponse;
import kr.hs.study.carrot.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

// 화면용 로그인/회원가입. 로그인하면 JWT 를 HttpOnly 쿠키에 담는다(스크립트에서 읽을 수 없음).
// ponytail: CSRF 방어는 SameSite=Lax 쿠키에만 의존한다. 폼 제출이 늘면 CSRF 토큰을 도입
@Controller
@RequiredArgsConstructor
public class AuthViewController {

    public static final String COOKIE = "access_token";
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final MemberService memberService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String next, Model model) {
        model.addAttribute("next", safeNext(next));
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        @RequestParam(required = false) String next,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            issueCookie(memberService.login(new LoginRequest(email, password)), request, response);
            return "redirect:" + safeNext(next);
        } catch (ResponseStatusException e) {
            model.addAttribute("error", e.getReason());
            model.addAttribute("email", email);
            model.addAttribute("next", safeNext(next));
            return "login";
        }
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String email, @RequestParam String nickname,
                         @RequestParam String password, @RequestParam String passwordConfirm,
                         HttpServletRequest request, HttpServletResponse response, Model model) {
        String error = validate(email, nickname, password, passwordConfirm);
        if (error == null) {
            try {
                memberService.signup(new SignupRequest(email, password, nickname, null));
                // 가입 직후 바로 로그인 상태로 시작
                issueCookie(memberService.login(new LoginRequest(email, password)), request, response);
                return "redirect:/";
            } catch (ResponseStatusException e) {
                error = e.getReason();
            }
        }
        model.addAttribute("error", error);
        model.addAttribute("email", email);
        model.addAttribute("nickname", nickname);
        return "signup";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO, request).toString());
        return "redirect:/login";
    }

    private String validate(String email, String nickname, String password, String passwordConfirm) {
        if (!EMAIL.matcher(email.strip()).matches() || email.strip().length() > 100) return "이메일 형식이 올바르지 않아요.";
        int len = nickname.strip().length();
        if (len < 2 || len > 20) return "닉네임은 2~20자로 입력해 주세요.";
        if (!password.equals(passwordConfirm)) return "비밀번호가 서로 달라요.";
        return null; // 비밀번호 길이는 MemberService 가 검사한다
    }

    private void issueCookie(LoginResponse login, HttpServletRequest request, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(login.accessToken(), Duration.ofSeconds(login.expiresInSeconds()), request).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge, HttpServletRequest request) {
        return ResponseCookie.from(COOKIE, value)
                .httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/").maxAge(maxAge).build();
    }

    // 로그인 후 돌아갈 주소는 이 사이트 안의 경로만 허용(오픈 리다이렉트 방지)
    private String safeNext(String next) {
        boolean ok = next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\")
                && !next.startsWith("/login") && !next.startsWith("/signup");
        return ok ? next : "/";
    }
}
