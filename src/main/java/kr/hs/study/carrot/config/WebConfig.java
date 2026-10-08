package kr.hs.study.carrot.config;

import kr.hs.study.carrot.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ViewAuthInterceptor viewAuthInterceptor;
    private final ProductImageService productImageService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(viewAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/signup", "/logout", "/css/**", "/js/**", "/uploads/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api/**", "/ws-chat/**", "/error", "/favicon.ico");
    }

    // 업로드한 사진을 /uploads/{파일명} 으로 제공한다. 파일명이 UUID 라 주소를 아는 사람만 볼 수 있다
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = productImageService.root().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .setCachePeriod(3600);
    }
}
