package com.example.devcoast.config;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final NotificacionInterceptor notificacionInterceptor;
    private final Path imagenesFeria;

    public WebMvcConfig(NotificacionInterceptor notificacionInterceptor,
            @Value("${devcoast.feria.datos:feria/datos}") String carpetaFeria) {
        this.notificacionInterceptor = notificacionInterceptor;
        this.imagenesFeria = Path.of(carpetaFeria, "imagenes").toAbsolutePath().normalize();
    }

    // logos, fotos e imágenes de proyectos de los datos curados de la feria (feria/datos/imagenes), servidos tal cual desde el disco
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/feria-img/**")
                .addResourceLocations(imagenesFeria.toUri().toString())
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(notificacionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/CSS/**", "/JS/**", "/img/**", "/fonts/**",
                        "/webjars/**", "/favicon.ico", "/feria-img/**", "/salud", "/interno/**",
                        "/login", "/register/**", "/register-options",
                        "/api/**");
    }
}
