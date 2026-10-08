package kr.hs.study.carrot.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Swagger UI: /swagger-ui.html, OpenAPI JSON: /v3/api-docs. 기본은 모든 API가 로그인 필요(🔒)이고, 공개 API는 컨트롤러에서 @SecurityRequirements 로 풀어 준다
@Configuration
@OpenAPIDefinition(
        security = @SecurityRequirement(name = "bearerAuth"),
        info = @Info(title = "NearDeal API", version = "v1", description = """
                동네 기반 중고거래 서비스 NearDeal의 REST API입니다.

                **사용 순서**: ① `POST /api/auth/signup` 으로 가입 → ② `POST /api/auth/login` 응답의 `accessToken` 복사 → \
                ③ 오른쪽 위 **Authorize** 에 붙여 넣기(`Bearer ` 는 쓰지 않습니다) → ④ 🔒 표시된 API 호출.

                **에러 형식**: 실패하면 `{status, error, message, path}` JSON 이 옵니다(입력 검증 실패는 `fieldErrors` 로 항목별 이유도 포함). 400 입력 오류 / 401 로그인 필요·토큰 만료 / \
                403 권한 없음 / 404 없음 / 409 중복 / 502 AI 서비스 오류.

                **WebSocket(STOMP)**: 실시간 채팅은 OpenAPI 로 표현할 수 없어 여기에 없습니다. 접속 `/ws-chat`(CONNECT 헤더 \
                `Authorization: Bearer 토큰`), 구독 `/topic/chat-rooms/{roomId}`, 전송 `/app/chat-rooms/{roomId}/messages`.

                **AI 상품 설명**: Google Gemini(Spring AI `ChatClient`)를 사용합니다. 키는 환경변수 `GEMINI_API_KEY` 입니다."""))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
        description = "로그인 응답의 accessToken 값만 붙여 넣으세요.")
public class OpenApiConfig {
}
