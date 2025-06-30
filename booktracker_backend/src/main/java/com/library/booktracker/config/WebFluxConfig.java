package com.library.booktracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                  // expose every endpoint
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
