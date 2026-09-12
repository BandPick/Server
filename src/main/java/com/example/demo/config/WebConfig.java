package com.example.demo.config;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,https://client-51ks.onrender.com}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        Set<String> origins = new LinkedHashSet<>();
        origins.add("http://localhost:3000");
        origins.add("http://127.0.0.1:3000");
        origins.add("https://client-51ks.onrender.com");
        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .map(this::stripTrailingSlash)
                .filter(origin -> !origin.isEmpty())
                .forEach(origins::add);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.copyOf(origins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    private String stripTrailingSlash(String origin) {
        if (origin.endsWith("/")) {
            return origin.substring(0, origin.length() - 1);
        }
        return origin;
    }
}
