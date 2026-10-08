package kr.hs.study.carrot.dto.response;

// 이후 요청마다 헤더 "Authorization: Bearer {accessToken}"으로 보낸다
public record LoginResponse(@io.swagger.v3.oas.annotations.media.Schema(description = "Authorize 에 붙여 넣는 값") String accessToken, @io.swagger.v3.oas.annotations.media.Schema(example = "Bearer") String tokenType, long expiresInSeconds, MemberResponse member) {
}
