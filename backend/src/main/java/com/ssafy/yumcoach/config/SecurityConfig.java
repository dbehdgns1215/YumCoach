package com.ssafy.yumcoach.config;

import com.ssafy.yumcoach.auth.filter.JwtAuthenticationFilter;
import com.ssafy.yumcoach.auth.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception {

                http
                                // REST + JWT면 보통 CSRF 끔
                                .csrf(csrf -> csrf.disable())

                                // 세션 안 씀
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // (선택) 기본 로그인/베이직 끄기
                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())

                                // (중요) 인증/인가 실패 시 응답코드 명확히
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((req, res, e) -> res.sendError(401))
                                                .accessDeniedHandler((req, res, e) -> res.sendError(403)))

                                .authorizeHttpRequests(auth -> auth
                                                // CORS 프리플라이트 허용
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                                // 공개 엔드포인트
                                                .requestMatchers("/", "/health", "/error").permitAll()
                                                .requestMatchers("/auth/**").permitAll()
                                                .requestMatchers("/api/user/signin", "/api/user/signup", "/api/user/refresh").permitAll()
                                                .requestMatchers("/api/community").permitAll()
                                                // 결제 승인 콜백/연동은 인증 없이 접근 가능해야 함
                                                .requestMatchers("/api/payments/**").permitAll()


                                                // Swagger/Springdoc
                                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                                                // 정적 리소스 (필요시)
                                                .requestMatchers("/favicon.ico", "/css/**", "/js/**", "/images/**")
                                                .permitAll()

                                                // 그 외는 인증 필요
                                                .anyRequest().authenticated())

                                .addFilterBefore(
                                                new JwtAuthenticationFilter(jwtUtil),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
