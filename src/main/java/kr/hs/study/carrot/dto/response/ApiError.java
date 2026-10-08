package kr.hs.study.carrot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

// REST API의 모든 실패 응답 형식. 내부 정보(스택트레이스, SQL, 클래스 이름)는 절대 담지 않는다
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "실패 응답")
public record ApiError(
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "입력값이 올바르지 않습니다.") String message,
        @Schema(example = "/api/products") String path,
        @Schema(description = "입력 검증에 실패한 항목별 이유(검증 실패일 때만)", example = "{\"title\":\"공백일 수 없습니다\"}") Map<String, String> fieldErrors
) {
}
