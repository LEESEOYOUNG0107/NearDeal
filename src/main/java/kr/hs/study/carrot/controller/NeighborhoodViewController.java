package kr.hs.study.carrot.controller;

import java.util.ArrayList;
import java.util.List;
import kr.hs.study.carrot.service.NeighborhoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;

// 내 동네는 최대 2개, 회원 계정(DB)에 저장하고 첫 번째가 현재 동네. 규칙 검사는 NeighborhoodService가 한다
@Controller
@RequiredArgsConstructor
public class NeighborhoodViewController {

    private static final int SEARCH_LIMIT = 200;

    private final NeighborhoodService neighborhoodService;

    @GetMapping("/my/neighborhood")
    public String page(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "false") boolean full,
                       @RequestAttribute("uid") Long uid, Model model) {
        List<String> mine = neighborhoodService.get(uid);
        String query = q.strip();
        List<String> results = neighborhoodService.search(query, SEARCH_LIMIT).stream()
                .filter(r -> !mine.contains(r))
                .toList();
        model.addAttribute("mine", mine);
        model.addAttribute("results", results);
        model.addAttribute("q", query);
        model.addAttribute("full", full);
        model.addAttribute("canAdd", NeighborhoodService.isValid(query) && !mine.contains(query) && !results.contains(query));
        return "neighborhood";
    }

    @PostMapping("/my/neighborhood/add")
    public String add(@RequestParam String region, @RequestAttribute("uid") Long uid) {
        String name = region.strip();
        List<String> mine = new ArrayList<>(neighborhoodService.get(uid));
        if (!NeighborhoodService.isValid(name) || mine.contains(name)) return "redirect:/my/neighborhood";
        if (mine.size() >= NeighborhoodService.MAX) return "redirect:/my/neighborhood?full=true";
        mine.add(name);
        neighborhoodService.set(uid, mine);
        return "redirect:/my/neighborhood";
    }

    @PostMapping("/my/neighborhood/remove")
    public String remove(@RequestParam String region, @RequestAttribute("uid") Long uid) {
        List<String> mine = new ArrayList<>(neighborhoodService.get(uid));
        if (mine.remove(region)) neighborhoodService.set(uid, mine);
        return "redirect:/my/neighborhood";
    }

    // 현재 동네 변경: 선택한 동네를 맨 앞으로
    @PostMapping("/my/neighborhood/activate")
    public String activate(@RequestParam String region, @RequestAttribute("uid") Long uid) {
        List<String> mine = new ArrayList<>(neighborhoodService.get(uid));
        if (mine.remove(region)) {
            mine.add(0, region);
            neighborhoodService.set(uid, mine);
        }
        return "redirect:/my/neighborhood";
    }
}
