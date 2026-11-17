package com.klu.spring.mvc.common;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter for downstream services: validates the token the gateway (or another
 * service) forwarded in the Authorization header and authenticates the request.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ERROR_ATTRIBUTE = "jwt.error";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            TokenValidationResult result = jwtUtil.validateToken(header);

            if (result.isValid()) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        result.getUsername(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + result.getRole())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                request.setAttribute(ERROR_ATTRIBUTE, result.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
