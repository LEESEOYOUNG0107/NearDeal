package kr.hs.study.carrot.config;

import static org.assertj.core.api.Assertions.assertThat;

import kr.hs.study.carrot.dto.response.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest http = new MockHttpServletRequest("POST", "/api/test");
    private final ServletWebRequest request = new ServletWebRequest(http);

    @Test
    void 예상하지_못한_오류는_내부_정보_없이_500_일반_문구만_돌려준다() {
        ResponseEntity<Object> res = handler.unexpected(new IllegalStateException("ORA-00001 secret table name"), request, http);

        ApiError body = (ApiError) res.getBody();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.message()).doesNotContain("ORA-").doesNotContain("secret");
        assertThat(body.path()).isEqualTo("/api/test");
    }

    @Test
    void 무결성_위반은_SQL_내용_없이_409다() {
        ResponseEntity<Object> res = handler.conflict(new DataIntegrityViolationException("ORA-00001: unique constraint (SYSTEM.UK123)"), request);

        ApiError body = (ApiError) res.getBody();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body.message()).doesNotContain("ORA-").doesNotContain("SYSTEM");
    }

    @Test
    void ResponseStatusException은_상태와_이유를_그대로_쓴다() {
        ResponseEntity<Object> res = handler.handleExceptionInternal(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 등록한 상품만 수정/삭제할 수 있습니다."),
                null, new org.springframework.http.HttpHeaders(), HttpStatus.FORBIDDEN, request);

        ApiError body = (ApiError) res.getBody();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body.status()).isEqualTo(403);
        assertThat(body.error()).isEqualTo("Forbidden");
        assertThat(body.message()).isEqualTo("본인이 등록한 상품만 수정/삭제할 수 있습니다.");
    }
}
