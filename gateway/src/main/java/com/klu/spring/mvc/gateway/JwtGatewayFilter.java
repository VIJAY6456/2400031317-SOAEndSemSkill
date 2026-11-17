package com.klu.spring.mvc.gateway;

import java.nio.charset.StandardCharsets;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.klu.spring.mvc.common.JwtUtil;
import com.klu.spring.mvc.common.TokenValidationResult;

import reactor.core.publisher.Mono;

/**
 * Runs for every request: validates the Bearer token, rejects bad ones with 401, and
 * forwards good ones to the target service (the Authorization header is passed through
 * unchanged, so the service can validate it again).
 */
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    public JwtGatewayFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Login and token-check endpoints are public
        if (path.startsWith("/auth/")) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        TokenValidationResult result = jwtUtil.validateToken(header);

        if (header == null || !header.startsWith("Bearer ") || !result.isValid()) {
            String reason = (header == null || !header.startsWith("Bearer "))
                    ? "Missing Bearer token" : result.getMessage();
            return unauthorized(exchange, reason);
        }

        // Add trusted user info for the services; strip anything the client tried to send
        ServerHttpRequest forwarded = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Name");
                    h.remove("X-User-Role");
                    h.add("X-User-Name", result.getUsername());
                    h.add("X-User-Role", result.getRole());
                })
                .build();

        return chain.filter(exchange.mutate().request(forwarded).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"service\":\"gateway\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
