package kr.hs.study.carrot.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

// 컨트롤러 파라미터에 붙이면 "지금 로그인한 회원의 번호"가 들어온다(JWT의 sub). 인증이 필요한 API에서만 쓴다
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@io.swagger.v3.oas.annotations.Parameter(hidden = true)
@AuthenticationPrincipal(expression = "T(java.lang.Long).valueOf(subject)")
public @interface CurrentUserId {
}
