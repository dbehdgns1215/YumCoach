package com.ssafy.yumcoach.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfigurer 구현체
 * CORS 설정은 CorsConfig + SecurityConfig에서 처리하므로 여기서는 제거.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS 설정은 CorsConfig.java에서 통합 관리
}
