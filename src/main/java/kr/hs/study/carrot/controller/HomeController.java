package kr.hs.study.carrot.controller;

import java.util.Comparator;
import java.util.List;
import kr.hs.study.carrot.dto.request.ProductSearchCondition;
import kr.hs.study.carrot.dto.response.ProductListResponse;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.service.FavoriteService;
import kr.hs.study.carrot.service.NeighborhoodService;
import kr.hs.study.carrot.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final FavoriteService favoriteService;
    private final NeighborhoodService neighborhoodService;

    @GetMapping("/")
    public String home(@RequestParam(required = false) Category category,
                       @RequestAttribute("uid") Long uid, Model model) {
        // 현재 동네(내 동네 목록의 첫 번째)가 있으면 그 동네 상품만 보여 준다
        String region = neighborhoodService.current(uid);
        List<ProductListResponse> latest = productService.getProducts(
                new ProductSearchCondition(null, category, null, null, null, region));
        // 인기 상품: 찜 수 → 조회수 순 상위 6개
        var favCounts = favoriteService.favoriteCounts();
        List<ProductListResponse> popular = latest.stream()
                .sorted(Comparator.<ProductListResponse>comparingLong(p -> favCounts.getOrDefault(p.id(), 0L))
                        .thenComparingInt(ProductListResponse::viewCount).reversed())
                .limit(6).toList();
        model.addAttribute("latest", latest);
        model.addAttribute("popular", popular);
        model.addAttribute("categories", Category.values());
        model.addAttribute("selected", category);
        return "home";
    }
}
