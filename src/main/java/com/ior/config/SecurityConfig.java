package com.ior.config;

import com.ior.filter.JwtAuthenticationFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
            // 禁用 CSRF（如果是前后端分离且使用 JWT，通常禁用 CSRF）
            .csrf(csrf -> csrf.disable())
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
                // 放行用户注册和登录接口
                .requestMatchers("/user/**", "/user/login").permitAll()
                // 放行 Hello World 测试接口
                .requestMatchers("/hello").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            // 在 UsernamePasswordAuthenticationFilter 之前添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
