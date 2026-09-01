package com.testlyflow.config;

import com.testlyflow.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final String adminPassword;

    public AdminAuthInterceptor(@Value("${app.admin.password}") String adminPassword) {
        this.adminPassword = adminPassword;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String provided = request.getHeader("X-Admin-Password");
        if (provided == null || !provided.equals(adminPassword)) {
            throw new UnauthorizedException("Неверный или отсутствующий пароль администратора");
        }
        return true;
    }
}
