package com.hotel.system.config;

import com.hotel.system.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns(
                        "/",              // 根路径（跳转房型页）
                        "/login",         // 登录页
                        "/register",      // 注册页
                        "/rooms",         // 房型浏览（未登录可看）
                        "/rooms/**",      // 房型详情
                        "/css/**",        // 静态资源
                        "/js/**",
                        "/images/**",
                        "/error",         // 默认错误页
                        "/403"            // 403越权提示页
                );
    }

    /**
     * 配置根路径重定向到房型浏览页
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/rooms");
    }

    /**
     * 配置静态资源映射，使 /images/** 指向 classpath:/static/images/
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
