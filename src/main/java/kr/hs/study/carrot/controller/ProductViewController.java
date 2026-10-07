package kr.hs.study.carrot.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Pattern;
import kr.hs.study.carrot.dto.request.ProductCreateRequest;
import kr.hs.study.carrot.dto.request.ProductSearchCondition;
import kr.hs.study.carrot.dto.response.ProductResponse;
import kr.hs.study.carrot.entity.Category;
import kr.hs.study.carrot.entity.ProductStatus;
import kr.hs.study.carrot.service.ChatService;
import kr.hs.study.carrot.service.FavoriteService;
import kr.hs.study.carrot.service.ProductImageService;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import kr.hs.study.carrot.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProductViewController {

    // 상태 변경 후 돌아갈 곳은 상품/채팅방 경로만 허용(오픈 리다이렉트 방지)
    private static final Pattern SAFE_BACK = Pattern.compile("^/(products|chat)/\\d+$");

    private final ProductService productService;
    private final FavoriteService favoriteService;
    private final ProductImageService productImageService;

    @GetMapping("/products")
    public String products(@ModelAttribute("condition") ProductSearchCondition condition, Model model) {
        model.addAttribute("products", productService.getProducts(condition));
        model.addAttribute("categories", Category.values());
        return "products";
    }

    @GetMapping("/products/new")
    public String form(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("categories", Category.values());
        if ("size".equals(error)) model.addAttribute("error", "사진 용량이 너무 커요. 한 장당 5MB 이하, 한 번에 55MB 이하로 올려 주세요.");
        return "product-form";
    }

    @PostMapping("/products")
    public String createProduct(
            @RequestAttribute("uid") Long uid,
            @RequestParam String title,
            @RequestParam Long price,
            @RequestParam Category category,
            @RequestParam String description,
            @RequestParam String region,
            @RequestParam(required = false) List<MultipartFile> images,
            Model model) {
        List<String> stored;
        try {
            stored = productImageService.store(images);
        } catch (ResponseStatusException e) {
            // 사진에 문제가 있으면 입력한 값은 그대로 두고 사진만 다시 고르게 한다
            model.addAttribute("categories", Category.values());
            model.addAttribute("error", e.getReason() + " 사진을 다시 선택해 주세요.");
            model.addAttribute("title", title);
            model.addAttribute("price", price);
            model.addAttribute("selectedCategory", category);
            model.addAttribute("description", description);
            model.addAttribute("region", region);
            return "product-form";
        }
        try {
            ProductResponse saved = productService.createProduct(
                    new ProductCreateRequest(title, price, category, description, region), uid);
            productImageService.attach(saved.id(), stored);
            return "redirect:/products/" + saved.id();
        } catch (RuntimeException e) {
            productImageService.deleteFiles(stored);
            throw e;
        }
    }

    // 요청 전체 용량이 설정(spring.servlet.multipart)을 넘으면 컨트롤러에 오기 전에 실패하므로 여기서 받는다
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooLarge() {
        return "redirect:/products/new?error=size";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, @RequestAttribute("uid") Long uid, Model model) {
        ProductResponse product = productService.getProduct(id);
        model.addAttribute("p", product);
        model.addAttribute("mine", product.sellerId().equals(uid));
        model.addAttribute("sellerName", ChatService.nameOf(product.sellerId()));
        model.addAttribute("images", productImageService.filenames(id));
        return "product-detail";
    }

    // 하트 버튼. 자바스크립트가 켜져 있으면 fetch(204), 아니면 일반 폼 제출 후 이전 페이지로 복귀
    @PostMapping("/products/{id}/favorite")
    public Object toggleFavorite(@PathVariable Long id, @RequestAttribute("uid") Long uid,
                                 @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                 @RequestHeader(value = "Referer", required = false) String referer,
                                 HttpServletRequest request) {
        favoriteService.toggle(id, uid);
        if (requestedWith != null) return ResponseEntity.noContent().build();
        String origin = request.getScheme() + "://" + request.getHeader("Host");
        return "redirect:" + (referer != null && referer.startsWith(origin + "/") ? referer.substring(origin.length()) : "/");
    }

    @PostMapping("/products/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam ProductStatus status,
                               @RequestParam(defaultValue = "") String back, @RequestAttribute("uid") Long uid) {
        productService.changeStatus(id, status, uid);
        return "redirect:" + (SAFE_BACK.matcher(back).matches() ? back : "/products/" + id);
    }
}
