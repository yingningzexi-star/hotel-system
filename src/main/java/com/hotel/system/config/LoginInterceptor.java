package com.hotel.system.config;

import com.hotel.system.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        // 1. 未登录拦截
        if (currentUser == null) {
            response.sendRedirect("/login");
            return false;
        }

        // 2. 权限拦截 (普通用户不允许访问管理员后台)
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/admin") && !"ADMIN".equals(currentUser.getRole())) {
            response.sendRedirect("/403");
            return false;
        }

        return true;
    }
}
