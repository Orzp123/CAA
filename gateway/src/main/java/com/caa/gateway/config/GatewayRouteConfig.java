package com.caa.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route configuration.
 * Routes all traffic to the backend service.
 * URI is overridable via backend.service.url property or BACKEND_SERVICE_URL env var.
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator routes(
            RouteLocatorBuilder builder,
            @Value("${backend.service.url:http://localhost:8081}") String backendUrl) {
        return builder.routes()
                .route("backend", r -> r.path("/**").uri(backendUrl))
                .build();
    }
}
