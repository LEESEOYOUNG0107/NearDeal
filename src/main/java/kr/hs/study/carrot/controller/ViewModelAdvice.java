package kr.hs.study.carrot.controller;

import java.util.Map;
import java.util.Set;
import kr.hs.study.carrot.service.FavoriteService;
import kr.hs.study.carrot.service.NeighborhoodService;
import kr.hs.study.carrot.service.NotificationService;
import kr.hs.study.carrot.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// 모든 뷰에서 쓰는 카테고리 라벨/아이콘, 내 찜 목록, 상품별 찜 수, 현재 동네, 안 읽은 알림 수
@ControllerAdvice(assignableTypes = {HomeController.class, ProductViewController.class, ChatViewController.class,
        MyViewController.class, SearchViewController.class, NotificationViewController.class, NeighborhoodViewController.class})
@RequiredArgsConstructor
public class ViewModelAdvice {

    static final Map<String, String> LABELS = Map.of(
            "DIGITAL", "디지털", "FURNITURE", "가구", "CLOTHES", "의류", "BEAUTY", "뷰티", "SPORTS", "스포츠",
            "BOOKS", "도서", "GAME", "게임", "FOOD", "식품", "ETC", "기타");
    private static final Map<String, String> ICONS = Map.of(
            "DIGITAL", "📱", "FURNITURE", "🛋️", "CLOTHES", "👕", "BEAUTY", "💄", "SPORTS", "⚽",
            "BOOKS", "📚", "GAME", "🎮", "FOOD", "🍪", "ETC", "📦");

    private final FavoriteService favoriteService;
    private final NotificationService notificationService;
    private final ProductImageService productImageService;
    private final NeighborhoodService neighborhoodService;

    @ModelAttribute("categoryLabels")
    Map<String, String> categoryLabels() {
        return LABELS;
    }

    @ModelAttribute("categoryIcons")
    Map<String, String> categoryIcons() {
        return ICONS;
    }

    @ModelAttribute("favIds")
    Set<Long> favIds(@RequestAttribute("uid") Long uid) {
        return favoriteService.favoriteProductIds(uid);
    }

    @ModelAttribute("favCounts")
    Map<Long, Long> favCounts() {
        return favoriteService.favoriteCounts();
    }

    // 상품 id -> 대표 사진 파일명(사진이 없는 상품은 키가 없다)
    @ModelAttribute("thumbs")
    Map<Long, String> thumbs() {
        return productImageService.covers();
    }

    // 내 동네 목록의 첫 번째가 현재 동네. 없으면 null(전체 동네)
    @ModelAttribute("activeRegion")
    String activeRegion(@RequestAttribute("uid") Long uid) {
        return neighborhoodService.current(uid);
    }

    @ModelAttribute("unreadNotices")
    long unreadNotices(@RequestAttribute("uid") Long uid) {
        return notificationService.unreadCount(uid);
    }
}
