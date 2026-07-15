package com.hotel.system.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        FlashMap outputFlashMap = RequestContextUtils.getOutputFlashMap(request);
        if (outputFlashMap != null) {
            outputFlashMap.put("error", e.getMessage());
        } else {
            request.getSession().setAttribute("flash_error", e.getMessage());
        }

        String redirectPath = resolveRedirectPath(request);
        return "redirect:" + redirectPath;
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception e, HttpServletRequest request) {
        log.error("未捕获的异常: {} - {}", e.getClass().getName(), e.getMessage(), e);

        String detail = "[" + e.getClass().getSimpleName() + "] " + e.getMessage();
        FlashMap outputFlashMap = RequestContextUtils.getOutputFlashMap(request);
        if (outputFlashMap != null) {
            outputFlashMap.put("error", detail);
        } else {
            request.getSession().setAttribute("flash_error", detail);
        }

        String redirectPath = resolveRedirectPath(request);
        return "redirect:" + redirectPath;
    }

    private String resolveRedirectPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            try {
                URI uri = new URI(referer);
                return uri.getPath() + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
            } catch (Exception ex) {
                // fallback: use request URI
            }
        }
        // fallback: redirect to rooms
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/admin")) {
            return "/admin/dashboard";
        }
        if (requestURI.startsWith("/orders")) {
            return "/orders";
        }
        return "/rooms";
    }
}
