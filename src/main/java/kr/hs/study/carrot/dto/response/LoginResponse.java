package kr.hs.study.carrot.dto.response;

// 이후 요청마다 헤더 "Authorization: Bearer {accessToken}"으로 보낸다
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, MemberResponse member) {
}
