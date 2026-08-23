package com.example.devcoast.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final NotificacionInterceptor notificacionInterceptor;

    public WebMvcConfig(NotificacionInterceptor notificacionInterceptor) {
        this.notificacionInterceptor = notificacionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(notificacionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/CSS/**", "/JS/**", "/img/**", "/fonts/**",
                        "/webjars/**", "/favicon.ico",
                        "/login", "/register/**", "/register-options",
                        "/api/**");
    }
}
