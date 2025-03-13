package com.resumehelp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**") // ✅ Allow all /api paths
                        .allowedOrigins("https://ai-resume-frontend-mg.vercel.app") // ✅ Your Vercel frontend URL
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // ✅ Allow necessary HTTP methods
                        .allowedHeaders("*") // ✅ Allow all headers
                        .allowCredentials(true); // ✅ Optional if cookies/session are needed
            }
        };
    }
}
