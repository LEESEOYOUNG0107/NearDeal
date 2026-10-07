package kr.hs.study.carrot.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import kr.hs.study.carrot.repository.ProductRepository;
import kr.hs.study.carrot.util.CookieList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 내 동네는 최대 2개, 쿠키에 저장하고 첫 번째가 현재 동네.
// ponytail: 회원 테이블에 동네 컬럼이 생기면 쿠키 대신 DB에 저장
@Controller
@RequiredArgsConstructor
public class NeighborhoodViewController {

    static final String COOKIE = "regions";
    private static final int MAX_REGIONS = 2;
    private static final Pattern VALID = Pattern.compile("^[0-9A-Za-z가-힣 ]{1,20}$");
    // ponytail: 동네 검색용 기본 목록(샘플). 행정구역 데이터가 생기면 교체. 상품에 쓰인 지역은 자동으로 합쳐진다
    private static final List<String> KNOWN = List.of("성수동", "연남동", "망원동", "합정동", "이태원동", "한남동",
            "서교동", "역삼동", "삼성동", "잠실동", "신림동", "상암동");

    private final ProductRepository productRepository;

    @GetMapping("/my/neighborhood")
    public String page(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "false") boolean full,
                       @CookieValue(name = COOKIE, required = false) String raw, Model model) {
        List<String> mine = CookieList.read(raw);
        var all = new LinkedHashSet<>(KNOWN);
        all.addAll(productRepository.findDistinctRegions());
        String query = q.strip();
        List<String> results = all.stream()
                .filter(r -> !mine.contains(r) && (query.isEmpty() || r.contains(query)))
                .toList();
        model.addAttribute("mine", mine);
        model.addAttribute("results", results);
        model.addAttribute("q", query);
        model.addAttribute("full", full);
        model.addAttribute("canAdd", VALID.matcher(query).matches() && !mine.contains(query) && !all.contains(query));
        return "neighborhood";
    }

    @PostMapping("/my/neighborhood/add")
    public String add(@RequestParam String region, @CookieValue(name = COOKIE, required = false) String raw,
                      HttpServletResponse response) {
        String name = region.strip();
        List<String> mine = new ArrayList<>(CookieList.read(raw));
        if (!VALID.matcher(name).matches()) return "redirect:/my/neighborhood";
        if (mine.contains(name)) return "redirect:/my/neighborhood";
        if (mine.size() >= MAX_REGIONS) return "redirect:/my/neighborhood?full=true";
        mine.add(name);
        CookieList.write(response, COOKIE, mine);
        return "redirect:/my/neighborhood";
    }

    @PostMapping("/my/neighborhood/remove")
    public String remove(@RequestParam String region, @CookieValue(name = COOKIE, required = false) String raw,
                         HttpServletResponse response) {
        List<String> mine = new ArrayList<>(CookieList.read(raw));
        mine.remove(region);
        CookieList.write(response, COOKIE, mine);
        return "redirect:/my/neighborhood";
    }

    // 현재 동네 변경: 선택한 동네를 맨 앞으로
    @PostMapping("/my/neighborhood/activate")
    public String activate(@RequestParam String region, @CookieValue(name = COOKIE, required = false) String raw,
                           HttpServletResponse response) {
        List<String> mine = new ArrayList<>(CookieList.read(raw));
        if (mine.remove(region)) {
            mine.add(0, region);
            CookieList.write(response, COOKIE, mine);
        }
        return "redirect:/my/neighborhood";
    }
}
