package kr.hs.study.carrot.controller;

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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(memberService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@CurrentUserId Long userId) {
        return ResponseEntity.ok(memberService.getMember(userId));
    }
}
