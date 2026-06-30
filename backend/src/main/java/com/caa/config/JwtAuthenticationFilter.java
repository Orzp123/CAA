package com.caa.config;

import com.caa.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 从 caa_token cookie（优先）或 Authorization: Bearer header（降级）解析 JWT，
 * 将 accountType claim 映射为 Spring Security GrantedAuthority 写入 SecurityContext。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null) {
            try {
                Jwt jwt = tokenService.parse(token);
                String accountId   = jwt.getSubject();
                String accountType = jwt.getClaimAsString("accountType");

                if (accountId != null && accountType != null) {
                    var authority = new SimpleGrantedAuthority("ROLE_" + accountType);
                    var auth = new UsernamePasswordAuthenticationToken(
                            accountId, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException ignored) {
                // 无效 token — SecurityContext 保持空，Spring Security 返回 401
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String fromCookie = Arrays.stream(cookies)
                    .filter(c -> "caa_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst().orElse(null);
            if (fromCookie != null) return fromCookie;
        }
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
