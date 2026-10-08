package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.NotBlank;

// 로그인에서는 형식/길이를 따지지 않는다. 틀리면 "이메일 또는 비밀번호가 올바르지 않습니다" 하나로만 답한다
public record LoginRequest(
        @io.swagger.v3.oas.annotations.media.Schema(example = "rabbit@example.com") @NotBlank String email,
        @io.swagger.v3.oas.annotations.media.Schema(example = "my-secret-pw") @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}
