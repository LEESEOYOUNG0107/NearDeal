package kr.hs.study.carrot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import kr.hs.study.carrot.dto.request.LoginRequest;
import kr.hs.study.carrot.dto.request.SignupRequest;
import kr.hs.study.carrot.dto.response.LoginResponse;
import kr.hs.study.carrot.dto.response.MemberResponse;
import kr.hs.study.carrot.security.CurrentUserId;
import kr.hs.study.carrot.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. 인증", description = "회원가입, 로그인, 내 정보")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @Operation(summary = "회원가입",
            description = "이메일은 소문자로 저장되고 이미 가입된 이메일이면 409입니다. 비밀번호는 BCrypt로 암호화해 저장하며 응답에는 포함되지 않습니다.")
    @SecurityRequirements
    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.signup(request));
    }

    @Operation(summary = "로그인",
            description = "성공하면 JWT accessToken을 돌려줍니다. 이메일이나 비밀번호가 틀리면 어느 쪽이 틀렸는지 알려 주지 않고 401입니다.")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }

    @Operation(summary = "내 정보",
            description = "토큰의 주인(로그인한 회원)의 정보와 내 동네 목록을 돌려줍니다.")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@CurrentUserId Long userId) {
        return ResponseEntity.ok(memberService.getMember(userId));
    }
}
