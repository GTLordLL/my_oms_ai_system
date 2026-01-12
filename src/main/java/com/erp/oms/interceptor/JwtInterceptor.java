package com.erp.oms.interceptor;

import com.erp.oms.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        String uri = request.getRequestURI();

        // 1. 放行 OPTIONS 请求（跨域预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 【核心修复】清洗多余的 Bearer 前缀（处理 Dify 重试叠加 Bug）
        String token = authHeader;
        if (token != null) {
            // 只要开头还有 "Bearer "，就一直截取，直到只剩下纯 Token
            while (token.toLowerCase().startsWith("bearer ")) {
                token = token.substring(7).trim();
            }

            try {
                // 3. 解析并校验清洗后的纯净 Token
                Claims claims = JwtUtils.parseToken(token);
                request.setAttribute("currentUserId", claims.get("userId"));
                return true;
            } catch (Exception e) {
                System.err.println("Token 校验失败: " + e.getMessage());
            }
        }

        // 4. 【备选方案】针对 Dify 所在服务器的内网 IP 开启白名单
        // 只要是本地 Docker 容器发来的请求，哪怕 Token 还是碎的，也直接放行
        String remoteAddr = request.getRemoteAddr();
        if ("172.17.0.1".equals(remoteAddr) || "127.0.0.1".equals(remoteAddr)) {
            System.out.println("检测到内网 Dify 请求，自动授权放行: " + uri);
            return true;
        }

        // 5. 校验失败，返回 401
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(401);
        response.getWriter().write("Unauthorized: Please login first.");
        return false;
    }
}