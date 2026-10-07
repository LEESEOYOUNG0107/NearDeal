package kr.hs.study.carrot.controller;

import jakarta.validation.Valid;
import kr.hs.study.carrot.dto.request.ProductDescriptionRequest;
import kr.hs.study.carrot.dto.response.ProductDescriptionResponse;
import kr.hs.study.carrot.service.AiProductDescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiProductDescriptionService aiProductDescriptionService;

    // ponytail: 인증/호출 횟수 제한 없음(로그인 전 단계). JWT 적용 후 사용자별 rate limit 필요
    @PostMapping("/product-description")
    public ResponseEntity<ProductDescriptionResponse> generateProductDescription(
            @Valid @RequestBody ProductDescriptionRequest request) {
        return ResponseEntity.ok(aiProductDescriptionService.generate(request));
    }
}
