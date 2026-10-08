package kr.hs.study.carrot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "7. AI", description = "Google Gemini(Spring AI ChatClient)로 상품 설명 초안 만들기")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiProductDescriptionService aiProductDescriptionService;

    // ponytail: 로그인은 필요하지만 호출 횟수 제한은 없다. 사용자별 rate limit 필요
    @Operation(summary = "AI 상품 설명 만들기",
            description = "상품 정보를 보내면 Gemini가 설명 초안을 만듭니다. 상품은 자동으로 등록되지 않고 글만 돌려줍니다. AI 서비스 오류는 502입니다.")
    @PostMapping("/product-description")
    public ResponseEntity<ProductDescriptionResponse> generateProductDescription(
            @Valid @RequestBody ProductDescriptionRequest request) {
        return ResponseEntity.ok(aiProductDescriptionService.generate(request));
    }
}
