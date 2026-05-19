package com.ior.config;

import com.ior.filter.JwtAuthenticationFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Resource
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 密码编码器：使用 BCrypt 强哈希算法
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（前后端分离 JWT 方案必须禁用）
            .csrf(csrf -> csrf.disable())
            // 配置 CORS（允许跨域和自定义 Header）
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOrigins(java.util.List.of("*")); // 生产环境建议指定具体域名
                corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                corsConfig.setAllowedHeaders(java.util.List.of("*"));
                corsConfig.setAllowCredentials(true);
                return corsConfig;
            }))
            // 设置为无状态会话（JWT 核心配置）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 放行 Swagger/Knife4j 相关资源
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-resources/**",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    "/doc.html"
                ).permitAll()
                // 放行用户注册、登录、发送验证码和重置密码接口
                .requestMatchers("/user/register", "/user/login", "/user/sendcode", "/user/password/reset").permitAll()
                // 放行 Hello World 测试接口
                .requestMatchers("/hello").permitAll()
                // 其他所有请求都需要认证（只要 Token 有效即可访问）
                .anyRequest().authenticated()
            )
            // 在 UsernamePasswordAuthenticationFilter 之前添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
