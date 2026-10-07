package kr.hs.study.carrot.config;

import java.util.List;
import kr.hs.study.carrot.security.JwtAuthenticationEntryPoint;
import kr.hs.study.carrot.security.JwtProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtProvider jwtProvider,
                                                   JwtAuthenticationEntryPoint entryPoint) throws Exception {
        // CSRF는 쿠키가 아니라 Authorization 헤더의 토큰으로 인증하는 API에는 해당하지 않아 끈다
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 공개 API: 회원가입, 로그인, 상품 목록/검색, 상품 상세(숫자 id만)
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/{id:[0-9]+}").permitAll()
                        // 나머지 /api/** 는 전부 로그인 필요. 새 API를 만들어도 기본적으로 잠겨 있다
                        .requestMatchers("/api/**").authenticated()
                        // ponytail: Thymeleaf 화면과 정적 파일, WebSocket 접속은 열어 둔다. 화면은 로그인 화면이 생기기 전까지
                        // 쿠키 uid(개발용)를 쓰므로 로그인 화면 도입 시 authenticated로 바꿔야 한다. WebSocket은 CONNECT 때 토큰을 검사한다
                        .anyRequest().permitAll())
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtProvider.decoder()))
                        .authenticationEntryPoint(entryPoint));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
