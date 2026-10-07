package kr.hs.study.carrot.controller;

import kr.hs.study.carrot.dto.response.ProductListResponse;
import java.time.LocalDateTime;
import java.util.List;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.ProductStatus;
import kr.hs.study.carrot.repository.ChatRoomRepository;
import kr.hs.study.carrot.repository.ProductRepository;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import kr.hs.study.carrot.service.ChatService;
import kr.hs.study.carrot.service.FavoriteService;
import kr.hs.study.carrot.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MyViewController {

    private final ProductService productService;
    private final FavoriteService favoriteService;
    private final ProductRepository productRepository;
    private final ChatRoomRepository chatRoomRepository;

    @GetMapping("/my")
    public String my(@RequestAttribute("uid") Long uid, Model model) {
        var mine = productService.getMyProducts(uid);
        model.addAttribute("meName", ChatService.nameOf(uid));
        model.addAttribute("meId", uid);
        model.addAttribute("sellingCount", mine.size());
        model.addAttribute("favoriteCount", favoriteService.getMyFavorites(uid).size());
        model.addAttribute("soldCount", mine.stream().filter(p -> p.status() == ProductStatus.SOLD_OUT).count());
        return "my";
    }

    @GetMapping("/my/favorites")
    public String myFavorites(@RequestAttribute("uid") Long uid, Model model) {
        model.addAttribute("latest", favoriteService.getMyFavorites(uid));
        return "my-favorites";
    }

    @GetMapping("/my/products")
    public String myProducts(@RequestAttribute("uid") Long uid, Model model) {
        model.addAttribute("latest", productService.getMyProducts(uid));
        return "my-products";
    }

    public record TradeRow(Long productId, String title, Long price, Category category, ProductStatus status,
                           String peer, LocalDateTime date) {
        boolean done() {
            return status == ProductStatus.SOLD_OUT;
        }
    }

    // ponytail: Product 에 구매자(buyerId) 컬럼이 없어서 정확한 거래 상대를 알 수 없다.
    // 판매 내역은 "채팅한 사람 수", 구매 내역은 "내가 채팅한 상품"으로 보여 준다. 거래 완료 시 구매자를 저장하면 정확해진다
    @GetMapping("/my/trades")
    public String trades(@RequestParam(defaultValue = "sell") String tab,
                         @RequestParam(defaultValue = "all") String filter,
                         @RequestAttribute("uid") Long uid, Model model) {
        boolean sell = !"buy".equals(tab);
        List<TradeRow> rows;
        if (sell) {
            rows = productRepository.findBySellerIdOrderByCreatedAtDesc(uid).stream()
                    .map(p -> {
                        int chats = chatRoomRepository.findByProductId(p.getId()).size();
                        return new TradeRow(p.getId(), p.getTitle(), p.getPrice(), p.getCategory(), p.getStatus(),
                                chats == 0 ? "채팅 없음" : "채팅 " + chats + "명", p.getCreatedAt());
                    }).toList();
        } else {
            rows = chatRoomRepository.findAllByParticipant(uid).stream()
                    .filter(r -> r.getBuyerId().equals(uid))
                    .map(r -> new TradeRow(r.getProduct().getId(), r.getProduct().getTitle(), r.getProduct().getPrice(),
                            r.getProduct().getCategory(), r.getProduct().getStatus(),
                            ChatService.nameOf(r.getSellerId()), r.getProduct().getCreatedAt()))
                    .toList();
        }
        List<TradeRow> shown = switch (filter) {
            case "ongoing" -> rows.stream().filter(r -> !r.done()).toList();
            case "done" -> rows.stream().filter(TradeRow::done).toList();
            default -> rows;
        };
        model.addAttribute("rows", shown);
        model.addAttribute("tab", sell ? "sell" : "buy");
        model.addAttribute("filter", filter);
        return "trades";
    }
}
