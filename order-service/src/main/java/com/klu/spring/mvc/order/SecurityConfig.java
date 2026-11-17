package com.klu.spring.mvc.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

import com.klu.spring.mvc.common.JwtAuthFilter;
import com.klu.spring.mvc.common.JwtUtil;

/**
 * The order service validates the forwarded token itself, then forwards the same
 * token to book-service when it calls it.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtUtil jwtUtil(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.expiration-ms}") long expirationMs) {
        return new JwtUtil(secret, expirationMs);
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/error").permitAll()
                .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint((request, response, e) -> {
                    Object reason = request.getAttribute(JwtAuthFilter.ERROR_ATTRIBUTE);
                    String message = reason != null ? reason.toString() : "Missing or invalid token";
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"service\":\"order-service\"}");
                })
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied\",\"service\":\"order-service\"}");
                })
            .and()
            .addFilterBefore(new JwtAuthFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
