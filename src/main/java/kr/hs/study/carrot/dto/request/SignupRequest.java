package kr.hs.study.carrot.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 비밀번호 길이는 @Size가 아니라 서비스에서 검사한다(@Size는 위반 시 입력값이 오류 메시지/로그에 찍힘)
public record SignupRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank String password,
        @NotBlank @Size(min = 2, max = 20) String nickname,
        @Size(max = 500) @Pattern(regexp = "^https?://\\S+$", message = "프로필 이미지는 http(s) 주소여야 합니다") String profileImage
) {
    @Override
    public String toString() {
        return "SignupRequest[email=" + email + ", nickname=" + nickname + ", password=***]";
    }
}
