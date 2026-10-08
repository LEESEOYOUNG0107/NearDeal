package kr.hs.study.carrot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.hs.study.carrot.dto.response.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// /api/** (RestController) 의 모든 예외를 {status, error, message, path[, fieldErrors]} JSON 하나로 통일한다.
// Thymeleaf 화면 컨트롤러에는 적용되지 않는다. 401은 JwtAuthenticationEntryPoint 가 같은 모양으로 만든다
@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 스프링 MVC가 던지는 예외(검증, JSON 오류, 405, 415, 404, ResponseStatusException 등)는 전부 여기를 지난다
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = ex instanceof BindException b ? fieldErrors(b) : null;
        return respond(status, headers, messageOf(ex, status), fields, request);
    }

    // ?minPrice=abc 처럼 쿼리 파라미터 객체 바인딩이 실패한 경우(기본 처리기에는 없어서 따로 받는다)
    @ExceptionHandler(BindException.class)
    ResponseEntity<Object> bind(BindException ex, WebRequest request) {
        return respond(HttpStatus.BAD_REQUEST, new HttpHeaders(), "입력값이 올바르지 않습니다.", fieldErrors(ex), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Object> constraint(ConstraintViolationException ex, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> fields.putIfAbsent(lastNode(v.getPropertyPath().toString()), v.getMessage()));
        return respond(HttpStatus.BAD_REQUEST, new HttpHeaders(), "입력값이 올바르지 않습니다.", fields, request);
    }

    // 유니크 제약 위반(같은 찜/채팅방을 동시에 두 번 만든 경우 등). SQL 내용은 응답에 넣지 않고 로그에만 남긴다
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> conflict(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("데이터 무결성 위반: {}", ex.getMostSpecificCause().getMessage());
        return respond(HttpStatus.CONFLICT, new HttpHeaders(), "이미 처리되었거나 중복된 요청입니다.", null, request);
    }

    // 예상하지 못한 오류. 원인은 서버 로그에만 남기고 클라이언트에는 일반 문구만 보낸다
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex, WebRequest request, HttpServletRequest http) {
        log.error("처리되지 않은 예외 {} {}", http.getMethod(), http.getRequestURI(), ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, new HttpHeaders(), "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", null, request);
    }

    private String messageOf(Exception ex, HttpStatusCode status) {
        return switch (ex) {
            case ResponseStatusException e -> e.getReason() != null ? e.getReason() : phrase(status);
            case BindException e -> "입력값이 올바르지 않습니다.";
            case HttpMessageNotReadableException e -> "요청 본문을 읽을 수 없습니다. JSON 형식과 값(예: category 이름)을 확인해 주세요.";
            case TypeMismatchException e -> "값의 형식이 올바르지 않습니다: " + (e.getPropertyName() != null ? e.getPropertyName() : "요청 값");
            case MissingServletRequestParameterException e -> "필수 값이 없습니다: " + e.getParameterName();
            case HttpMediaTypeNotSupportedException e -> "지원하지 않는 Content-Type 입니다. application/json 으로 보내 주세요.";
            case MaxUploadSizeExceededException e -> "업로드 용량이 너무 큽니다.";
            default -> status.is5xxServerError() ? "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요." : phrase(status);
        };
    }

    private Map<String, String> fieldErrors(BindException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError f : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(f.getField(), f.getDefaultMessage());
        }
        return fields.isEmpty() ? null : fields;
    }

    private String lastNode(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private ResponseEntity<Object> respond(HttpStatusCode status, HttpHeaders headers, String message,
                                           @Nullable Map<String, String> fields, WebRequest request) {
        String path = request instanceof ServletWebRequest s ? s.getRequest().getRequestURI() : null;
        return new ResponseEntity<>(new ApiError(status.value(), phrase(status), message, path, fields), headers, status);
    }

    private String phrase(HttpStatusCode status) {
        HttpStatus s = HttpStatus.resolve(status.value());
        return s != null ? s.getReasonPhrase() : "Error";
    }
}
