package com.ior.filter;

import com.ior.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    Claims claims = jwtUtil.getClaimsFromToken(token);
                    String role = (String) claims.get("role");
                    
                    // 构造权限列表
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (role != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }

                    // 将用户信息和权限存入 SecurityContext
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(claims.get("userId"), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    // 校验失败（过期或黑名单），强制清除认证状态
                    SecurityContextHolder.clearContext();
                    log.warn("JWT 校验失败，已清除安全上下文");
                }
            } catch (Exception e) {
                log.error("JWT 解析异常", e);
                SecurityContextHolder.clearContext();
                // Token 无效，不做处理，让后续 Security 拦截
            }
        }

        filterChain.doFilter(request, response);
    }
}
