package kr.hs.study.carrot.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.hs.study.carrot.dto.request.ProductSearchCondition;
import kr.hs.study.carrot.dto.response.ProductListResponse;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.ProductStatus;
import kr.hs.study.carrot.service.FavoriteService;
import kr.hs.study.carrot.service.ProductService;
import kr.hs.study.carrot.util.CookieList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class SearchViewController {

    private static final String RECENT_COOKIE = "recent";
    private static final int MAX_RECENT = 8;
    private static final List<String> SUGGESTED = List.of("아이패드", "자전거", "에어팟", "노트북", "책상", "닌텐도");
    private static final Map<String, String> STATUS_LABELS = Map.of("SALE", "판매중", "RESERVED", "예약중", "SOLD_OUT", "거래완료");

    private final ProductService productService;
    private final FavoriteService favoriteService;

    public record FilterChip(String label, String removeHref) {
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String keyword,
                         @RequestParam(required = false) Category category,
                         @RequestParam(required = false) Long minPrice,
                         @RequestParam(required = false) Long maxPrice,
                         @RequestParam(required = false) ProductStatus status,
                         @RequestParam(required = false) String region,
                         @RequestParam(defaultValue = "latest") String sort,
                         @CookieValue(name = RECENT_COOKIE, required = false) String recentRaw,
                         HttpServletResponse response, Model model) {
        String kw = StringUtils.hasText(keyword) ? keyword.strip() : null;
        String rg = StringUtils.hasText(region) ? region.strip() : null;

        // 현재 조건을 순서대로 담아 두고, 칩의 해제 링크는 "그 항목만 뺀" 주소로 만든다
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", kw);
        params.put("category", category);
        params.put("minPrice", minPrice);
        params.put("maxPrice", maxPrice);
        params.put("status", status);
        params.put("region", rg);
        params.put("sort", "latest".equals(sort) ? null : sort);
        boolean searched = params.entrySet().stream().anyMatch(e -> e.getValue() != null && !e.getKey().equals("sort"));

        List<String> recent = new ArrayList<>(CookieList.read(recentRaw));
        if (kw != null) {
            recent.remove(kw);
            recent.add(0, kw);
            if (recent.size() > MAX_RECENT) recent = new ArrayList<>(recent.subList(0, MAX_RECENT));
            CookieList.write(response, RECENT_COOKIE, recent);
        }

        List<FilterChip> chips = new ArrayList<>();
        if (category != null) chips.add(new FilterChip("카테고리 " + ViewModelAdvice.LABELS.get(category.name()), hrefWithout(params, "category")));
        if (minPrice != null || maxPrice != null) {
            chips.add(new FilterChip(priceLabel(minPrice, maxPrice), hrefWithout(params, "minPrice", "maxPrice")));
        }
        if (status != null) chips.add(new FilterChip(STATUS_LABELS.get(status.name()), hrefWithout(params, "status")));
        if (rg != null) chips.add(new FilterChip(rg, hrefWithout(params, "region")));

        List<ProductListResponse> results = List.of();
        if (searched) {
            results = new ArrayList<>(productService.getProducts(
                    new ProductSearchCondition(kw, category, minPrice, maxPrice, status, rg)));
            var favCounts = favoriteService.favoriteCounts();
            // ponytail: 정렬은 조회된 결과를 메모리에서 한다. 결과가 수천 건이 넘으면 DB 정렬과 페이지 처리로 교체
            switch (sort) {
                case "priceAsc" -> results.sort(Comparator.comparing(ProductListResponse::price));
                case "priceDesc" -> results.sort(Comparator.comparing(ProductListResponse::price).reversed());
                case "popular" -> results.sort(Comparator.<ProductListResponse>comparingLong(p -> favCounts.getOrDefault(p.id(), 0L)).reversed());
                default -> { }
            }
        }

        model.addAttribute("searched", searched);
        model.addAttribute("results", results);
        model.addAttribute("chips", chips);
        model.addAttribute("keyword", kw);
        model.addAttribute("category", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("status", status);
        model.addAttribute("region", rg);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", Category.values());
        model.addAttribute("recent", kw != null ? List.of() : recent);
        model.addAttribute("suggested", SUGGESTED);
        return "search";
    }

    @PostMapping("/search/recent/clear")
    public String clearRecent(HttpServletResponse response) {
        CookieList.write(response, RECENT_COOKIE, List.of());
        return "redirect:/search";
    }

    private static String hrefWithout(Map<String, Object> params, String... exclude) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/search");
        List<String> skip = List.of(exclude);
        params.forEach((k, v) -> {
            if (v != null && !skip.contains(k)) builder.queryParam(k, v);
        });
        return builder.encode().build().toUriString();
    }

    private static String priceLabel(Long min, Long max) {
        if (min != null && max != null) return won(min) + " ~ " + won(max);
        return min != null ? won(min) + " 이상" : won(max) + " 이하";
    }

    private static String won(long v) {
        return v >= 10_000 && v % 10_000 == 0 ? v / 10_000 + "만원" : String.format("%,d원", v);
    }
}
