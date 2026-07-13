package com.hotel.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns(
                        "/login",         // 登录页
                        "/register",      // 注册页
                        "/api/auth/**",   // 登录注册接口
                        "/css/**",        // 静态资源
                        "/js/**",
                        "/images/**",
                        "/error",         // 默认错误页
                        "/403"            // 403越权提示页
                );
    }
}
