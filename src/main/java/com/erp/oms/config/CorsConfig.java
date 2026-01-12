package com.erp.oms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有域名跨域访问，生产环境建议写具体的域名
        config.addAllowedOriginPattern("*");
        // 允许发送 Cookie (如果后续涉及登录 Session)
        config.setAllowCredentials(true);
        // 允许所有的请求头
        config.addAllowedHeader("*");
        // 允许所有的请求方法 (GET, POST, PUT, DELETE)
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}