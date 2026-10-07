package kr.hs.study.carrot.dto.response;

// 이 상품의 찜 개수와, 지금 사용자가 찜했는지 여부
public record FavoriteStatusResponse(long favoriteCount, boolean favorited) {
}
