package kr.hs.study.carrot.controller;

import jakarta.validation.Valid;
import java.util.Map;
import kr.hs.study.carrot.dto.request.ProductDescriptionRequest;
import kr.hs.study.carrot.service.AiProductDescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// 판매글 화면의 "AI로 설명 쓰기"가 부르는 주소. /api/** 는 Authorization 헤더(JWT)가 있어야 해서
// 쿠키로 로그인한 화면에서는 401 이 났다. 이 주소는 화면과 같은 쿠키 로그인 검사(ViewAuthInterceptor)를 따른다.
// 실패하면 {"message": "..."} 로 원인을 화면에 알려 준다(내부 오류 내용은 서버 로그에만 남기고 화면에는 내보내지 않는다)
@RestController
@RequiredArgsConstructor
public class AiViewController {

    private static final String NO_KEY = "no-key-set";

    private final AiProductDescriptionService aiProductDescriptionService;

    @Value("${spring.ai.google.genai.api-key:" + NO_KEY + "}")
    private String apiKey;

    @PostMapping("/ai/description")
    public ResponseEntity<?> generate(@Valid @RequestBody ProductDescriptionRequest request) {
        if (apiKey == null || apiKey.isBlank() || NO_KEY.equals(apiKey)) {
            return error(HttpStatus.SERVICE_UNAVAILABLE,
                    "서버에 GEMINI_API_KEY가 설정되지 않았어요. 환경변수를 설정한 뒤 서버를 다시 시작해 주세요.");
        }
        try {
            return ResponseEntity.ok(aiProductDescriptionService.generate(request));
        } catch (ResponseStatusException e) {
            return error(HttpStatus.BAD_GATEWAY, "AI 서버 호출에 실패했어요. API 키나 모델 설정을 확인해 주세요. (서버 로그 참고)");
        }
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
